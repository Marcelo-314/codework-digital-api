package com.codeworkdigital.api.contact.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeworkdigital.api.support.PostgreSqlIntegrationTestSupport;
import com.codeworkdigital.api.verification.application.HumanVerificationContext;
import com.codeworkdigital.api.verification.application.HumanVerificationService;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
class AdminContactSubmissionApiIntegrationTest extends PostgreSqlIntegrationTestSupport {

    private static final String ADMIN_USERNAME = "admin-test";
    private static final String ADMIN_PASSWORD = "admin-test-password-not-real";
    private static final String VALID_HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

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
    void adminRequestWithoutAuthenticationIsRejected() throws Exception {
        HttpResponse<String> response = getAdmin(null, 0, 20);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("www-authenticate")).isPresent();
    }

    @Test
    void adminRequestWithInvalidCredentialsIsRejected() throws Exception {
        HttpResponse<String> response = getAdmin(basicAuth(ADMIN_USERNAME, "wrong-password"), 0, 20);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void adminRequestWithValidCredentialsReturnsPagedSubmissionsOrderedByCreatedAtDesc() throws Exception {
        UUID oldest = insertSubmission("Ada Lovelace", "ada@example.test", Instant.parse("2026-08-04T10:00:00Z"));
        UUID newest = insertSubmission("Grace Hopper", "grace@example.test", Instant.parse("2026-08-04T12:00:00Z"));
        UUID middle = insertSubmission("Katherine Johnson", "katherine@example.test", Instant.parse("2026-08-04T11:00:00Z"));

        HttpResponse<String> response = getAdmin(basicAuth(ADMIN_USERNAME, ADMIN_PASSWORD), 0, 2);
        Map<String, Object> body = json(response);
        List<?> content = (List<?>) body.get("content");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type")).contains("application/json");
        assertThat(body.get("page")).isEqualTo(0);
        assertThat(body.get("size")).isEqualTo(2);
        assertThat(body.get("totalElements")).isEqualTo(3);
        assertThat(body.get("totalPages")).isEqualTo(2);
        assertThat(content).hasSize(2);
        assertThat(ids(content)).containsExactly(newest.toString(), middle.toString());
        assertThat(ids(content)).doesNotContain(oldest.toString());
        assertThat(((Map<?, ?>) content.getFirst()).get("locale")).isEqualTo("es");
        assertThat(response.body()).doesNotContain(
                "payloadHash",
                "payload_hash",
                "idempotencyKey",
                "turnstile",
                "secret",
                VALID_HASH);
    }

    @Test
    void adminSubmissionsWithSameCreatedAtAreOrderedByIdDesc() throws Exception {
        Instant sameTimestamp = Instant.parse("2026-08-04T10:00:00Z");
        UUID lowerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID higherId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        insertSubmission(lowerId, "Ada Lovelace", "ada@example.test", sameTimestamp);
        insertSubmission(higherId, "Grace Hopper", "grace@example.test", sameTimestamp);

        HttpResponse<String> response = getAdmin(basicAuth(ADMIN_USERNAME, ADMIN_PASSWORD), 0, 20);
        Map<String, Object> body = json(response);
        List<?> content = (List<?>) body.get("content");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(ids(content)).containsExactly(higherId.toString(), lowerId.toString());
    }

    @Test
    void adminSecondPageReturnsRemainingSubmission() throws Exception {
        UUID older = insertSubmission("Ada Lovelace", "ada@example.test", Instant.parse("2026-08-04T10:00:00Z"));
        insertSubmission("Grace Hopper", "grace@example.test", Instant.parse("2026-08-04T12:00:00Z"));

        HttpResponse<String> response = getAdmin(basicAuth(ADMIN_USERNAME, ADMIN_PASSWORD), 1, 1);
        Map<String, Object> body = json(response);
        List<?> content = (List<?>) body.get("content");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.get("page")).isEqualTo(1);
        assertThat(body.get("size")).isEqualTo(1);
        assertThat(body.get("totalElements")).isEqualTo(2);
        assertThat(body.get("totalPages")).isEqualTo(2);
        assertThat(ids(content)).containsExactly(older.toString());
    }

    @Test
    void adminPageSizeIsCappedDefensively() throws Exception {
        insertSubmission("Ada Lovelace", "ada@example.test", Instant.parse("2026-08-04T10:00:00Z"));

        HttpResponse<String> response = getAdmin(basicAuth(ADMIN_USERNAME, ADMIN_PASSWORD), 0, 500);
        Map<String, Object> body = json(response);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.get("size")).isEqualTo(100);
    }

    @Test
    void adminRejectsInvalidPaginationParameters() throws Exception {
        HttpResponse<String> negativePage = getAdmin(basicAuth(ADMIN_USERNAME, ADMIN_PASSWORD), -1, 20);
        HttpResponse<String> zeroSize = getAdmin(basicAuth(ADMIN_USERNAME, ADMIN_PASSWORD), 0, 0);

        assertProblem(negativePage, "page");
        assertProblem(zeroSize, "size");
    }

    @Test
    void publicContactSubmissionPostRemainsPublic() throws Exception {
        HttpResponse<String> response = postPublic(UUID.randomUUID().toString(), """
                {
                  "source": "HOME",
                  "locale": "es",
                  "name": "Ada Lovelace",
                  "email": "ada@example.test",
                  "phone": null,
                  "companyOrProject": null,
                  "message": "Project message",
                  "turnstileToken": "valid-token"
                }
                """);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(rowCount()).isEqualTo(1);
        assertThat(humanVerificationService.contexts).containsExactly(HumanVerificationContext.CONTACT_HOME);
    }

    private HttpResponse<String> getAdmin(String authorization, int page, int size)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri("/api/admin/contact-submissions?page=" + page + "&size=" + size))
                .GET();
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postPublic(String idempotencyKey, String body)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri("/api/v1/contact-submissions"))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private UUID insertSubmission(String name, String email, Instant timestamp) {
        UUID id = UUID.randomUUID();
        insertSubmission(id, name, email, timestamp);
        return id;
    }

    private void insertSubmission(UUID id, String name, String email, Instant timestamp) {
        jdbcClient.sql("""
                        INSERT INTO contact_submission (
                            id,
                            idempotency_key,
                            payload_hash,
                            source,
                            locale,
                            name,
                            email,
                            phone,
                            company_or_project,
                            message,
                            status,
                            created_at,
                            updated_at
                        )
                        VALUES (
                            :id,
                            :idempotency_key,
                            :payload_hash,
                            'HOME',
                            'ES',
                            :name,
                            :email,
                            null,
                            null,
                            'Project message',
                            'RECEIVED',
                            :created_at,
                            :updated_at
                        )
                        """)
                .param("id", id)
                .param("idempotency_key", UUID.randomUUID())
                .param("payload_hash", VALID_HASH)
                .param("name", name)
                .param("email", email)
                .param("created_at", OffsetDateTime.ofInstant(timestamp, ZoneOffset.UTC))
                .param("updated_at", OffsetDateTime.ofInstant(timestamp, ZoneOffset.UTC))
                .update();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> json(HttpResponse<String> response) throws Exception {
        return objectMapper.readValue(response.body(), Map.class);
    }

    private List<String> ids(List<?> content) {
        return content.stream()
                .map(entry -> ((Map<?, ?>) entry).get("id").toString())
                .toList();
    }

    private void assertProblem(HttpResponse<String> response, String field) throws Exception {
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("content-type")).contains("application/problem+json");
        Map<String, Object> body = json(response);
        assertThat(body.get("code")).isEqualTo("validation_failed");
        assertThat(body.get("instance")).isEqualTo("/api/admin/contact-submissions");
        assertThat((List<?>) body.get("errors")).singleElement().satisfies(error -> {
            Map<?, ?> entry = (Map<?, ?>) error;
            assertThat(entry.get("field")).isEqualTo(field);
            assertThat(entry.get("code")).isEqualTo("invalid");
        });
    }

    private int rowCount() {
        return jdbcClient.sql("SELECT count(*) FROM contact_submission").query(Integer.class).single();
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private String basicAuth(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
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
        }

        void reset() {
            contexts.clear();
        }
    }
}
