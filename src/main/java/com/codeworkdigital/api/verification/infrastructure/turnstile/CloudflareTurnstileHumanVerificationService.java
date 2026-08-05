package com.codeworkdigital.api.verification.infrastructure.turnstile;

import com.codeworkdigital.api.verification.application.HumanVerificationContext;
import com.codeworkdigital.api.verification.application.HumanVerificationRejectedException;
import com.codeworkdigital.api.verification.application.HumanVerificationService;
import com.codeworkdigital.api.verification.application.HumanVerificationUnavailableException;
import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class CloudflareTurnstileHumanVerificationService implements HumanVerificationService {

    private static final Set<String> REJECTED_CODES = Set.of(
            "invalid-input-response",
            "missing-input-response",
            "timeout-or-duplicate");
    private static final Set<String> UNAVAILABLE_CODES = Set.of(
            "missing-input-secret",
            "invalid-input-secret",
            "bad-request",
            "internal-error");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final TurnstileProperties properties;

    public CloudflareTurnstileHumanVerificationService(
            HttpClient turnstileHttpClient,
            ObjectMapper objectMapper,
            TurnstileProperties properties) {
        this.httpClient = turnstileHttpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void verify(String token, HumanVerificationContext context) {
        HttpRequest request = HttpRequest.newBuilder(properties.siteverifyUrl())
                .timeout(properties.requestTimeout())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(formBody(token)))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new HumanVerificationUnavailableException(exception);
        } catch (IOException exception) {
            throw new HumanVerificationUnavailableException(exception);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new HumanVerificationUnavailableException();
        }
        classify(response.body(), context);
    }

    private String formBody(String token) {
        return "secret=" + encode(properties.secretKey()) + "&response=" + encode(token);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void classify(String body, HumanVerificationContext context) {
        JsonNode root;
        try {
            root = objectMapper.readValue(body, JsonNode.class);
        } catch (JacksonException exception) {
            throw new HumanVerificationUnavailableException(exception);
        }

        JsonNode success = root.get("success");
        if (success == null || !success.isBoolean()) {
            throw new HumanVerificationUnavailableException();
        }

        Set<String> errorCodes = errorCodes(root);
        if (success.asBoolean()) {
            if (!errorCodes.isEmpty()) {
                throw new HumanVerificationUnavailableException();
            }
            requireExpectedAction(root, context);
            requireAllowedHostname(root);
            return;
        }

        if (errorCodes.stream().anyMatch(UNAVAILABLE_CODES::contains) || errorCodes.stream().anyMatch(code -> !REJECTED_CODES.contains(code))) {
            throw new HumanVerificationUnavailableException();
        }
        if (errorCodes.stream().anyMatch(REJECTED_CODES::contains)) {
            throw new HumanVerificationRejectedException();
        }
        throw new HumanVerificationUnavailableException();
    }

    private Set<String> errorCodes(JsonNode root) {
        JsonNode codes = root.get("error-codes");
        if (codes == null || codes.isNull()) {
            return Set.of();
        }
        if (!codes.isArray()) {
            return Set.of("__invalid_error_codes__");
        }
        java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>();
        for (JsonNode code : codes) {
            if (!code.isString()) {
                result.add("__invalid_error_code__");
            } else {
                result.add(code.stringValue());
            }
        }
        return Set.copyOf(result);
    }

    private void requireExpectedAction(JsonNode root, HumanVerificationContext context) {
        JsonNode action = root.get("action");
        if (action == null || !action.isString() || !properties.expectedAction(context).equals(action.stringValue())) {
            throw new HumanVerificationRejectedException();
        }
    }

    private void requireAllowedHostname(JsonNode root) {
        JsonNode hostname = root.get("hostname");
        if (hostname == null || !hostname.isString() || !properties.isAllowedHostname(hostname.stringValue())) {
            throw new HumanVerificationRejectedException();
        }
    }
}
