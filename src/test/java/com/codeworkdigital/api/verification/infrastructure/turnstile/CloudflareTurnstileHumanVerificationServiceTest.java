package com.codeworkdigital.api.verification.infrastructure.turnstile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeworkdigital.api.verification.application.HumanVerificationContext;
import com.codeworkdigital.api.verification.application.HumanVerificationRejectedException;
import com.codeworkdigital.api.verification.application.HumanVerificationUnavailableException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class CloudflareTurnstileHumanVerificationServiceTest {

    private static final String SECRET = "test-secret-not-real";
    private static final String TOKEN = "test-token-not-real";
    private static final String RESPONSE_BODY = "{\"provider\":\"body\"}";

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsExpectedSiteverifyRequest() throws Exception {
        AtomicReference<CapturedRequest> captured = new AtomicReference<>();
        serviceWithServer(exchange -> {
            captured.set(capture(exchange));
            respond(exchange, 200, success("contact_home", "localhost"));
        }).verify(TOKEN, HumanVerificationContext.CONTACT_HOME);

        CapturedRequest request = captured.get();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.path()).isEqualTo("/siteverify");
        assertThat(request.headers().get("Content-type")).contains("application/x-www-form-urlencoded");
        assertThat(request.headers().get("Accept")).contains("application/json");
        assertThat(request.form()).containsEntry("secret", SECRET);
        assertThat(request.form()).containsEntry("response", TOKEN);
        assertThat(request.form()).doesNotContainKeys("remoteip", "idempotency_key", "name", "email", "message");
    }

    @Test
    void acceptsSuccessfulHomeVerification() throws Exception {
        serviceResponding(200, success("contact_home", "localhost"))
                .verify(TOKEN, HumanVerificationContext.CONTACT_HOME);
    }

    @Test
    void acceptsSuccessfulContactPageVerification() throws Exception {
        serviceResponding(200, success("contact_page", "localhost"))
                .verify(TOKEN, HumanVerificationContext.CONTACT_PAGE);
    }

    @Test
    void rejectsWrongAction() throws Exception {
        assertRejected(success("contact_page", "localhost"), HumanVerificationContext.CONTACT_HOME);
    }

    @Test
    void rejectsMissingAction() throws Exception {
        assertRejected("{\"success\":true,\"hostname\":\"localhost\"}", HumanVerificationContext.CONTACT_HOME);
    }

    @Test
    void rejectsWrongHostname() throws Exception {
        assertRejected(success("contact_home", "example.test"), HumanVerificationContext.CONTACT_HOME);
    }

    @Test
    void rejectsMissingHostname() throws Exception {
        assertRejected("{\"success\":true,\"action\":\"contact_home\"}", HumanVerificationContext.CONTACT_HOME);
    }

    @Test
    void rejectsInvalidInputResponse() throws Exception {
        assertRejected(failure("invalid-input-response"), HumanVerificationContext.CONTACT_HOME);
    }

    @Test
    void rejectsTimeoutOrDuplicate() throws Exception {
        assertRejected(failure("timeout-or-duplicate"), HumanVerificationContext.CONTACT_HOME);
    }

    @Test
    void treatsInternalErrorAsUnavailable() throws Exception {
        assertUnavailable(failure("internal-error"));
    }

    @Test
    void treatsInvalidInputSecretAsUnavailable() throws Exception {
        assertUnavailable(failure("invalid-input-secret"));
    }

    @Test
    void treatsHttp500AsUnavailable() throws Exception {
        assertThatThrownBy(() -> serviceResponding(500, RESPONSE_BODY).verify(TOKEN, HumanVerificationContext.CONTACT_HOME))
                .isInstanceOf(HumanVerificationUnavailableException.class);
    }

    @Test
    void treatsHttp429AsUnavailable() throws Exception {
        assertThatThrownBy(() -> serviceResponding(429, RESPONSE_BODY).verify(TOKEN, HumanVerificationContext.CONTACT_HOME))
                .isInstanceOf(HumanVerificationUnavailableException.class);
    }

    @Test
    void treatsMalformedJsonAsUnavailable() throws Exception {
        assertUnavailable("{not-json");
    }

    @Test
    void treatsMissingSuccessAsUnavailable() throws Exception {
        assertUnavailable("{\"hostname\":\"localhost\",\"action\":\"contact_home\"}");
    }

    @Test
    void treatsRequestTimeoutAsUnavailable() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        CloudflareTurnstileHumanVerificationService service = serviceWithServer(exchange -> {
            try {
                release.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, Duration.ofMillis(100), Duration.ofMillis(100));

        assertThatThrownBy(() -> service.verify(TOKEN, HumanVerificationContext.CONTACT_HOME))
                .isInstanceOf(HumanVerificationUnavailableException.class);
        release.countDown();
    }

    @Test
    void exceptionsDoNotExposeTokenSecretOrProviderBody() throws Exception {
        assertThatThrownBy(() -> serviceResponding(500, RESPONSE_BODY).verify(TOKEN, HumanVerificationContext.CONTACT_HOME))
                .isInstanceOf(HumanVerificationUnavailableException.class)
                .hasMessageNotContaining(TOKEN)
                .hasMessageNotContaining(SECRET)
                .hasMessageNotContaining(RESPONSE_BODY);

        assertThatThrownBy(() -> serviceResponding(200, failure("invalid-input-response"))
                .verify(TOKEN, HumanVerificationContext.CONTACT_HOME))
                .isInstanceOf(HumanVerificationRejectedException.class)
                .hasMessageNotContaining(TOKEN)
                .hasMessageNotContaining(SECRET)
                .hasMessageNotContaining("invalid-input-response");
    }

    private void assertRejected(String body, HumanVerificationContext context) throws Exception {
        assertThatThrownBy(() -> serviceResponding(200, body).verify(TOKEN, context))
                .isInstanceOf(HumanVerificationRejectedException.class);
    }

    private void assertUnavailable(String body) throws Exception {
        assertThatThrownBy(() -> serviceResponding(200, body).verify(TOKEN, HumanVerificationContext.CONTACT_HOME))
                .isInstanceOf(HumanVerificationUnavailableException.class);
    }

    private CloudflareTurnstileHumanVerificationService serviceResponding(int status, String body) throws IOException {
        return serviceWithServer(exchange -> respond(exchange, status, body));
    }

    private CloudflareTurnstileHumanVerificationService serviceWithServer(ExchangeHandler handler) throws IOException {
        return serviceWithServer(handler, Duration.ofSeconds(1), Duration.ofSeconds(2));
    }

    private CloudflareTurnstileHumanVerificationService serviceWithServer(
            ExchangeHandler handler,
            Duration connectTimeout,
            Duration requestTimeout) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/siteverify", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();

        TurnstileProperties properties = new TurnstileProperties(
                SECRET,
                URI.create("http://localhost:" + server.getAddress().getPort() + "/siteverify"),
                Set.of("localhost"),
                "contact_home",
                "contact_page",
                connectTimeout,
                requestTimeout);
        return new CloudflareTurnstileHumanVerificationService(
                HttpClient.newBuilder()
                        .connectTimeout(connectTimeout)
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build(),
                new ObjectMapper(),
                properties);
    }

    private CapturedRequest capture(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return new CapturedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                exchange.getRequestHeaders(),
                parseForm(body));
    }

    private Map<String, String> parseForm(String body) {
        Map<String, String> form = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            form.put(decode(parts[0]), parts.length == 2 ? decode(parts[1]) : "");
        }
        return form;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private String success(String action, String hostname) {
        return "{\"success\":true,\"action\":\"" + action + "\",\"hostname\":\"" + hostname + "\"}";
    }

    private String failure(String code) {
        return "{\"success\":false,\"error-codes\":[\"" + code + "\"]}";
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private record CapturedRequest(
            String method,
            String path,
            com.sun.net.httpserver.Headers headers,
            Map<String, String> form) {
    }
}
