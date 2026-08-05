package com.codeworkdigital.api.contact.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeworkdigital.api.support.PostgreSqlIntegrationTestSupport;
import com.codeworkdigital.api.verification.application.HumanVerificationContext;
import com.codeworkdigital.api.verification.application.HumanVerificationRejectedException;
import com.codeworkdigital.api.verification.application.HumanVerificationService;
import com.codeworkdigital.api.verification.application.HumanVerificationUnavailableException;
import java.io.IOException;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.ObjectMapper;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContactSubmissionApiIntegrationTest extends PostgreSqlIntegrationTestSupport {

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";
    private static final String DISALLOWED_ORIGIN = "http://malicious.example.test";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FakeHumanVerificationService humanVerificationService;

    @BeforeEach
    void cleanDatabase() {
        jdbcClient.sql("DELETE FROM contact_submission").update();
        humanVerificationService.reset();
    }

    @Test
    void validHomeSubmissionCreatesRow() throws Exception {
        HttpResponse<String> response = post(UUID.randomUUID().toString(), validJson("HOME", "es", "Project message"));
        Map<String, Object> body = json(response);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("content-type")).contains("application/json");
        assertThat(body.get("submissionId")).isNotNull();
        assertThat(body.get("status")).isEqualTo("RECEIVED");
        assertThat(body.get("receivedAt")).isNotNull();
        assertThat(response.body()).doesNotContain("payloadHash", "idempotencyKey", "turnstileToken", "Ada", "ada@example.test");

        Map<String, Object> row = onlyRow();
        assertThat(row.get("source")).isEqualTo("HOME");
        assertThat(row.get("locale")).isEqualTo("ES");
        assertThat(row.get("status")).isEqualTo("RECEIVED");
        assertThat(humanVerificationService.contexts).containsExactly(HumanVerificationContext.CONTACT_HOME);
    }

    @Test
    void allowedPreflightAuthorizesOnlyConfiguredCorsContract() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri("/api/v1/contact-submissions"))
                .header("Origin", ALLOWED_ORIGIN)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type, Idempotency-Key")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isBetween(200, 299);
        assertThat(response.headers().firstValue("access-control-allow-origin")).contains(ALLOWED_ORIGIN);
        assertThat(response.headers().firstValue("access-control-allow-methods")).hasValueSatisfying(value ->
                assertThat(value).contains("POST"));
        assertThat(response.headers().firstValue("access-control-allow-headers")).hasValueSatisfying(value ->
                assertThat(value.toLowerCase()).contains("content-type", "idempotency-key"));
        assertThat(response.headers().firstValue("access-control-max-age")).contains("3600");
        assertThat(response.headers().firstValue("access-control-allow-credentials")).isEmpty();
        assertThat(response.headers().firstValue("vary")).isPresent();
        assertThat(humanVerificationService.contexts).isEmpty();
        assertThat(rowCount()).isZero();
    }

    @Test
    void allowedOriginPostIncludesExactCorsAuthorization() throws Exception {
        HttpResponse<String> response = postWithOrigin(
                UUID.randomUUID().toString(),
                validJson("HOME", "es", "Project message"),
                ALLOWED_ORIGIN);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("access-control-allow-origin")).contains(ALLOWED_ORIGIN);
        assertThat(response.headers().firstValue("access-control-allow-credentials")).isEmpty();
        assertDefensiveHeaders(response);
    }

    @Test
    void disallowedOriginIsRejectedBeforeVerification() throws Exception {
        HttpResponse<String> response = postWithOrigin(
                UUID.randomUUID().toString(),
                validJson("HOME", "es", "Project message"),
                DISALLOWED_ORIGIN);

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.headers().firstValue("access-control-allow-origin")).isEmpty();
        assertThat(humanVerificationService.contexts).isEmpty();
        assertThat(rowCount()).isZero();
    }

    @Test
    void arbitraryWildcardOriginIsNotAuthorized() throws Exception {
        HttpResponse<String> response = postWithOrigin(
                UUID.randomUUID().toString(),
                validJson("HOME", "es", "Project message"),
                "http://another-origin.example.test");

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.headers().firstValue("access-control-allow-origin")).isEmpty();
        assertThat(humanVerificationService.contexts).isEmpty();
        assertThat(rowCount()).isZero();
    }

    @Test
    void requestWithoutOriginContinuesToWork() throws Exception {
        HttpResponse<String> response = post(UUID.randomUUID().toString(), validJson("HOME", "es", "Project message"));

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("access-control-allow-origin")).isEmpty();
        assertThat(rowCount()).isEqualTo(1);
    }

    @Test
    void validContactPageSubmissionCreatesRow() throws Exception {
        HttpResponse<String> response = post(UUID.randomUUID().toString(), validJson("CONTACT_PAGE", "it", "Project message"));

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(onlyRow().get("source")).isEqualTo("CONTACT_PAGE");
        assertThat(humanVerificationService.contexts).containsExactly(HumanVerificationContext.CONTACT_PAGE);
    }

    @Test
    void normalizesPayloadBeforePersisting() throws Exception {
        String body = """
                {
                  "source": "HOME",
                  "locale": "en",
                  "name": "  Ada\\t  Lovelace  ",
                  "email": "Ada.Example@Example.TEST",
                  "phone": "  +39  123   456  ",
                  "companyOrProject": "   ",
                  "message": "  Line one\\r\\nLine  two\\rLine three  ",
                  "turnstileToken": "valid-token"
                }
                """;

        HttpResponse<String> response = post(UUID.randomUUID().toString(), body);

        assertThat(response.statusCode()).isEqualTo(201);
        Map<String, Object> row = onlyRow();
        assertThat(row.get("name")).isEqualTo("Ada Lovelace");
        assertThat(row.get("email")).isEqualTo("Ada.Example@Example.TEST");
        assertThat(row.get("phone")).isEqualTo("+39 123 456");
        assertThat(row.get("company_or_project")).isNull();
        assertThat(row.get("message")).isEqualTo("Line one\nLine  two\nLine three");
    }

    @Test
    void replaysSameKeyAndSameNormalizedPayload() throws Exception {
        String key = UUID.randomUUID().toString();
        HttpResponse<String> first = post(key, validJson("HOME", "es", "Project message", "valid-token"));
        String replayPayload = """
                {
                  "source": "HOME",
                  "locale": "es",
                  "name": " Ada   Lovelace ",
                  "email": "ada@example.test",
                  "phone": null,
                  "companyOrProject": null,
                  "message": "Project message",
                  "turnstileToken": "valid-token-replay"
                }
                """;

        HttpResponse<String> second = post(key, replayPayload);
        Map<String, Object> firstBody = json(first);
        Map<String, Object> secondBody = json(second);

        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(second.statusCode()).isEqualTo(200);
        assertThat(secondBody.get("submissionId")).isEqualTo(firstBody.get("submissionId"));
        assertThat(secondBody.get("receivedAt")).isEqualTo(firstBody.get("receivedAt"));
        assertThat(rowCount()).isEqualTo(1);
        assertThat(humanVerificationService.contexts).containsExactly(
                HumanVerificationContext.CONTACT_HOME,
                HumanVerificationContext.CONTACT_HOME);
    }

    @Test
    void rejectsSameKeyWithDifferentPayload() throws Exception {
        String key = UUID.randomUUID().toString();
        HttpResponse<String> first = post(key, validJson("HOME", "es", "Original message"));

        HttpResponse<String> conflict = post(key, validJson("HOME", "es", "Different message", "valid-token-conflict"));

        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(conflict.statusCode()).isEqualTo(409);
        assertThat(conflict.headers().firstValue("content-type")).contains("application/problem+json");
        assertThat(json(conflict).get("code")).isEqualTo("idempotency_conflict");
        assertThat(rowCount()).isEqualTo(1);
        assertThat(onlyRow().get("message")).isEqualTo("Original message");
        assertThat(humanVerificationService.contexts).hasSize(2);
    }

    @Test
    void differentKeysWithSamePayloadCreateDifferentRows() throws Exception {
        HttpResponse<String> first = post(UUID.randomUUID().toString(), validJson("HOME", "es", "Project message"));
        HttpResponse<String> second = post(UUID.randomUUID().toString(), validJson("HOME", "es", "Project message"));

        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(second.statusCode()).isEqualTo(201);
        assertThat(json(second).get("submissionId")).isNotEqualTo(json(first).get("submissionId"));
        assertThat(rowCount()).isEqualTo(2);
    }

    @Test
    void rejectsMissingIdempotencyKey() throws Exception {
        HttpResponse<String> response = postWithoutIdempotencyKey(validJson("HOME", "es", "Project message"));

        assertProblem(response, 400, "missing_idempotency_key", "/api/v1/contact-submissions");
        assertThat(rowCount()).isZero();
    }

    @Test
    void rejectsInvalidIdempotencyKey() throws Exception {
        HttpResponse<String> response = post("not-a-uuid", validJson("HOME", "es", "Project message"));

        assertProblem(response, 400, "invalid_idempotency_key", "/api/v1/contact-submissions");
        assertThat(rowCount()).isZero();
    }

    @Test
    void rejectsAbbreviatedOrWhitespaceUuid() throws Exception {
        HttpResponse<String> abbreviated = post("00000000-0000-0000-0000-00000000000", validJson("HOME", "es", "Project message"));
        HttpResponse<String> spaced = post(UUID.randomUUID().toString().replaceFirst("-", " -"), validJson("HOME", "es", "Project message"));

        assertProblem(abbreviated, 400, "invalid_idempotency_key", "/api/v1/contact-submissions");
        assertProblem(spaced, 400, "invalid_idempotency_key", "/api/v1/contact-submissions");
        assertThat(rowCount()).isZero();
    }

    @Test
    void rejectsMissingTurnstileToken() throws Exception {
        String body = validJson("HOME", "es", "Project message")
                .replace(",\n  \"turnstileToken\": \"valid-token\"", "");

        HttpResponse<String> response = post(UUID.randomUUID().toString(), body);

        assertValidationError(response, "turnstileToken", "required");
        assertThat(humanVerificationService.contexts).isEmpty();
    }

    @Test
    void rejectsBlankTurnstileToken() throws Exception {
        String body = validJson("HOME", "es", "Project message")
                .replace("\"turnstileToken\": \"valid-token\"", "\"turnstileToken\": \"   \"");

        HttpResponse<String> response = post(UUID.randomUUID().toString(), body);

        assertValidationError(response, "turnstileToken", "required");
        assertThat(humanVerificationService.contexts).isEmpty();
    }

    @Test
    void rejectsOversizedTurnstileTokenBeforeVerification() throws Exception {
        String body = validJson("HOME", "es", "Project message")
                .replace("valid-token", "t".repeat(2049));

        HttpResponse<String> response = post(UUID.randomUUID().toString(), body);

        assertValidationError(response, "turnstileToken", "invalid_length");
        assertThat(humanVerificationService.contexts).isEmpty();
    }

    @Test
    void rejectedHumanVerificationDoesNotPersist() throws Exception {
        String body = validJson("HOME", "es", "Project message")
                .replace("valid-token", "rejected-token");

        HttpResponse<String> response = post(UUID.randomUUID().toString(), body);

        assertProblem(response, 400, "human_verification_failed", "/api/v1/contact-submissions");
        assertThat(rowCount()).isZero();
        assertThat(response.body()).doesNotContain("rejected-token", "invalid-input-response", "hostname", "action");
    }

    @Test
    void unknownJsonFieldIsRejectedBeforeVerification() throws Exception {
        String body = validJson("HOME", "es", "Project message")
                .replace("\n}", ",\n  \"unexpectedField\": \"value\"\n}");

        HttpResponse<String> response = post(UUID.randomUUID().toString(), body);

        assertProblem(response, 400, "invalid_request", "/api/v1/contact-submissions");
        assertThat(humanVerificationService.contexts).isEmpty();
        assertThat(rowCount()).isZero();
    }

    @Test
    void rejectsOversizedBodyWithKnownContentLengthBeforeVerification() throws Exception {
        String body = validJson("HOME", "es", "Project message")
                .replace("Project message", "A".repeat(66000));

        HttpResponse<String> response = post(UUID.randomUUID().toString(), body);

        assertRequestTooLarge(response);
    }

    @Test
    void rejectsOversizedBodyWithUnknownContentLengthBeforeVerification() throws Exception {
        String body = validJson("HOME", "es", "Project message")
                .replace("Project message", "A".repeat(66000));

        HttpResponse<String> response = postWithBodyPublisher(
                UUID.randomUUID().toString(),
                unknownLengthPublisher(body));

        assertRequestTooLarge(response);
    }

    @Test
    void unavailableHumanVerificationReturnsServiceUnavailableAndDoesNotPersist() throws Exception {
        String body = validJson("HOME", "es", "Project message")
                .replace("valid-token", "unavailable-token");

        HttpResponse<String> response = post(UUID.randomUUID().toString(), body);

        assertProblem(response, 503, "human_verification_unavailable", "/api/v1/contact-submissions");
        assertThat(rowCount()).isZero();
        assertThat(response.body()).doesNotContain("unavailable-token", "hostname", "action");
    }

    @Test
    void rejectsInvalidEmail() throws Exception {
        String body = validJson("HOME", "es", "Project message").replace("ada@example.test", "invalid-email");

        HttpResponse<String> response = post(UUID.randomUUID().toString(), body);

        assertValidationError(response, "email", "invalid_email");
    }

    @Test
    void rejectsEmailInvalidAfterNormalization() throws Exception {
        String body = validJson("HOME", "es", "Project message").replace("ada@example.test", "  invalid-email  ");

        HttpResponse<String> response = post(UUID.randomUUID().toString(), body);

        assertValidationError(response, "email", "invalid_email");
    }

    @Test
    void rejectsBlankRequiredField() throws Exception {
        String body = validJson("HOME", "es", "Project message").replace("\"Ada Lovelace\"", "\"   \"");

        HttpResponse<String> response = post(UUID.randomUUID().toString(), body);

        assertValidationError(response, "name", "required");
    }

    @Test
    void rejectsNameMadeOnlyOfNoBreakSpacesAfterNormalization() throws Exception {
        String body = validJson("HOME", "es", "Project message").replace("\"Ada Lovelace\"", "\"\\u00A0\\u00A0\"");

        HttpResponse<String> response = post(UUID.randomUUID().toString(), body);

        assertOnlyValidationError(response, "name", "required");
        assertThat(response.body()).doesNotContain("persistence_error");
        assertThat(rowCount()).isZero();
    }

    @Test
    void rejectsMessageMadeOnlyOfFigureSpacesAfterNormalization() throws Exception {
        String body = validJson("HOME", "es", "Project message").replace("Project message", "\\u2007\\u2007");

        HttpResponse<String> response = post(UUID.randomUUID().toString(), body);

        assertOnlyValidationError(response, "message", "required");
        assertThat(rowCount()).isZero();
    }

    @Test
    void rejectsMessageMadeOnlyOfNarrowNoBreakSpacesAfterNormalization() throws Exception {
        String body = validJson("HOME", "es", "Project message").replace("Project message", "\\u202F\\u202F");

        HttpResponse<String> response = post(UUID.randomUUID().toString(), body);

        assertOnlyValidationError(response, "message", "required");
        assertThat(rowCount()).isZero();
    }

    @Test
    void acceptsValidEmailWithExteriorWhitespaceAfterNormalization() throws Exception {
        String body = validJson("HOME", "es", "Project message")
                .replace("ada@example.test", "  Ada.Example@Example.TEST  ");

        HttpResponse<String> response = post(UUID.randomUUID().toString(), body);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(onlyRow().get("email")).isEqualTo("Ada.Example@Example.TEST");
    }

    @Test
    void rejectsUnsupportedSource() throws Exception {
        HttpResponse<String> response = post(UUID.randomUUID().toString(), validJson("LANDING", "es", "Project message"));

        assertValidationError(response, "source", "unsupported_value");
    }

    @Test
    void rejectsUnsupportedLocale() throws Exception {
        HttpResponse<String> response = post(UUID.randomUUID().toString(), validJson("HOME", "pt", "Project message"));

        assertValidationError(response, "locale", "unsupported_value");
    }

    @Test
    void rejectsFieldsOverLength() throws Exception {
        String longName = "A".repeat(121);
        String body = validJson("HOME", "es", "Project message").replace("Ada Lovelace", " " + longName + " ");

        HttpResponse<String> response = post(UUID.randomUUID().toString(), body);

        assertValidationError(response, "name", "invalid_length");
        assertThat(rowCount()).isZero();
    }

    @Test
    void rejectsBlankSourceWithOnlyRequiredError() throws Exception {
        HttpResponse<String> response = post(UUID.randomUUID().toString(), validJson("", "es", "Project message"));

        assertOnlyValidationError(response, "source", "required");
    }

    @Test
    void rejectsBlankLocaleWithOnlyRequiredError() throws Exception {
        HttpResponse<String> response = post(UUID.randomUUID().toString(), validJson("HOME", "", "Project message"));

        assertOnlyValidationError(response, "locale", "required");
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        HttpResponse<String> response = post(UUID.randomUUID().toString(), "{\"source\":\"HOME\"");

        assertProblem(response, 400, "invalid_request", "/api/v1/contact-submissions");
    }

    @Test
    void rejectsUnsupportedContentType() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri("/api/v1/contact-submissions"))
                .header("Content-Type", "text/plain")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .POST(HttpRequest.BodyPublishers.ofString(validJson("HOME", "es", "Project message")))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertProblem(response, 415, "unsupported_media_type", "/api/v1/contact-submissions");
        assertThat(response.headers().firstValue("accept")).hasValueSatisfying(value ->
                assertThat(value).contains("application/json"));
    }

    @Test
    void rejectsGetOnSubmissionRoute() throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(uri("/api/v1/contact-submissions")).GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertProblem(response, 405, "method_not_allowed", "/api/v1/contact-submissions");
        assertThat(response.headers().firstValue("allow")).hasValueSatisfying(value ->
                assertThat(value).contains("POST"));
    }

    @Test
    void problemDetailsDoNotExposeInternalsOrRejectedValues() throws Exception {
        HttpResponse<String> response = post(UUID.randomUUID().toString(), validJson("HOME", "es", "invalid-email")
                .replace("ada@example.test", "invalid-email"));
        Map<String, Object> body = json(response);

        assertThat(body.get("type")).isEqualTo("urn:codeworkdigital:problem:validation_failed");
        assertThat(body.get("title")).isNotNull();
        assertThat(body.get("status")).isEqualTo(400);
        assertThat(body.get("detail")).isNotNull();
        assertThat(body.get("instance")).isEqualTo("/api/v1/contact-submissions");
        assertThat(body.get("code")).isEqualTo("validation_failed");
        assertThat(response.body()).doesNotContain("stackTrace", "exception", "rejectedValue", "invalid-email", "valid-token");
    }

    @Test
    void defensiveHeadersAreAppliedToValidationErrorsAndTurnstileUnavailable() throws Exception {
        HttpResponse<String> validation = post(UUID.randomUUID().toString(), validJson("HOME", "es", "Project message")
                .replace("ada@example.test", "invalid-email"));
        HttpResponse<String> unavailable = post(UUID.randomUUID().toString(), validJson("HOME", "es", "Project message")
                .replace("valid-token", "unavailable-token"));

        assertProblem(validation, 400, "validation_failed", "/api/v1/contact-submissions");
        assertProblem(unavailable, 503, "human_verification_unavailable", "/api/v1/contact-submissions");
        assertDefensiveHeaders(validation);
        assertDefensiveHeaders(unavailable);
    }

    @Test
    void successfulResponseDoesNotExposePayloadOrPersonalData() throws Exception {
        HttpResponse<String> response = post(UUID.randomUUID().toString(), validJson("HOME", "es", "Project message"));

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).doesNotContain(
                "payloadHash",
                "idempotencyKey",
                "turnstileToken",
                "name",
                "email",
                "phone",
                "companyOrProject",
                "message",
                "Ada",
                "ada@example.test");
    }

    private void assertValidationError(HttpResponse<String> response, String field, String code) throws Exception {
        assertProblem(response, 400, "validation_failed", "/api/v1/contact-submissions");
        Map<String, Object> body = json(response);
        assertThat((List<?>) body.get("errors")).anySatisfy(error -> {
            Map<?, ?> entry = (Map<?, ?>) error;
            assertThat(entry.get("field")).isEqualTo(field);
            assertThat(entry.get("code")).isEqualTo(code);
        });
        assertThat(rowCount()).isZero();
    }

    private void assertOnlyValidationError(HttpResponse<String> response, String field, String code) throws Exception {
        assertProblem(response, 400, "validation_failed", "/api/v1/contact-submissions");
        Map<String, Object> body = json(response);
        assertThat((List<?>) body.get("errors")).singleElement().satisfies(error -> {
            Map<?, ?> entry = (Map<?, ?>) error;
            assertThat(entry.get("field")).isEqualTo(field);
            assertThat(entry.get("code")).isEqualTo(code);
        });
        assertThat(response.body()).doesNotContain("unsupported_value", "rejectedValue", "persistence_error");
        assertThat(rowCount()).isZero();
    }

    private void assertProblem(HttpResponse<String> response, int status, String code, String instance) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("content-type")).contains("application/problem+json");
        Map<String, Object> body = json(response);
        assertThat(body.get("type")).isEqualTo("urn:codeworkdigital:problem:" + code);
        assertThat(body.get("title")).isNotNull();
        assertThat(body.get("status")).isEqualTo(status);
        assertThat(body.get("detail")).isNotNull();
        assertThat(body.get("instance")).isEqualTo(instance);
        assertThat(body.get("code")).isEqualTo(code);
        assertThat(response.body()).doesNotContain("stackTrace", "exception", "rejectedValue");
    }

    private void assertRequestTooLarge(HttpResponse<String> response) throws Exception {
        assertProblem(response, 413, "request_too_large", "/api/v1/contact-submissions");
        assertDefensiveHeaders(response);
        assertThat(humanVerificationService.contexts).isEmpty();
        assertThat(rowCount()).isZero();
        assertThat(response.body()).doesNotContain("66000", "65536", "valid-token", "Ada Lovelace", "ada@example.test");
    }

    private void assertDefensiveHeaders(HttpResponse<String> response) {
        assertThat(response.headers().firstValue("cache-control")).contains("no-store");
        assertThat(response.headers().firstValue("pragma")).contains("no-cache");
        assertThat(response.headers().firstValue("x-content-type-options")).contains("nosniff");
        assertThat(response.headers().firstValue("referrer-policy")).contains("no-referrer");
        assertThat(response.headers().firstValue("content-security-policy"))
                .contains("default-src 'none'; frame-ancestors 'none'");
    }

    private HttpResponse<String> post(String idempotencyKey, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri("/api/v1/contact-submissions"))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postWithOrigin(String idempotencyKey, String body, String origin)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri("/api/v1/contact-submissions"))
                .header("Origin", origin)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postWithBodyPublisher(String idempotencyKey, BodyPublisher publisher)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri("/api/v1/contact-submissions"))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .POST(publisher)
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postWithoutIdempotencyKey(String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri("/api/v1/contact-submissions"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private BodyPublisher unknownLengthPublisher(String body) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return new BodyPublisher() {
            @Override
            public long contentLength() {
                return -1;
            }

            @Override
            public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
                subscriber.onSubscribe(new Flow.Subscription() {
                    private boolean done;

                    @Override
                    public void request(long n) {
                        if (done) {
                            return;
                        }
                        done = true;
                        subscriber.onNext(ByteBuffer.wrap(bytes));
                        subscriber.onComplete();
                    }

                    @Override
                    public void cancel() {
                        done = true;
                    }
                });
            }
        };
    }

    private String validJson(String source, String locale, String message) {
        return validJson(source, locale, message, "valid-token");
    }

    private String validJson(String source, String locale, String message, String token) {
        return """
                {
                  "source": "%s",
                  "locale": "%s",
                  "name": "Ada Lovelace",
                  "email": "ada@example.test",
                  "phone": null,
                  "companyOrProject": null,
                  "message": "%s",
                  "turnstileToken": "%s"
                }
                """.formatted(source, locale, message, token);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> json(HttpResponse<String> response) throws Exception {
        return objectMapper.readValue(response.body(), Map.class);
    }

    private Map<String, Object> onlyRow() {
        return jdbcClient.sql("""
                        SELECT source,
                               locale,
                               name,
                               email,
                               phone,
                               company_or_project,
                               message,
                               status
                        FROM contact_submission
                        """)
                .query()
                .singleRow();
    }

    private int rowCount() {
        return jdbcClient.sql("SELECT count(*) FROM contact_submission").query(Integer.class).single();
    }

    @TestConfiguration
    static class HumanVerificationTestConfiguration {

        @Bean
        @Primary
        FakeHumanVerificationService fakeHumanVerificationService() {
            return new FakeHumanVerificationService();
        }
    }

    static class FakeHumanVerificationService implements HumanVerificationService {

        private final List<HumanVerificationContext> contexts = new ArrayList<>();

        @Override
        public void verify(String token, HumanVerificationContext context) {
            contexts.add(context);
            if ("rejected-token".equals(token)) {
                throw new HumanVerificationRejectedException();
            }
            if ("unavailable-token".equals(token)) {
                throw new HumanVerificationUnavailableException();
            }
        }

        void reset() {
            contexts.clear();
        }
    }
}
