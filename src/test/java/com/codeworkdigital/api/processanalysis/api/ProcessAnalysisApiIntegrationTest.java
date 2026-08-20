package com.codeworkdigital.api.processanalysis.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeworkdigital.api.processanalysis.application.AnalyzeProcessDescriptionCommand;
import com.codeworkdigital.api.processanalysis.application.InvalidProcessAnalysisModelResponseException;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisApplicationService;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisModelClient;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisModelResult;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisUnavailableException;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisStatus;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortEvidence;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortEvidenceProjectionMapper;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortEvidenceQuantity;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortEvidenceQuantityStatus;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortMaterialityAssessmentEvaluator;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortPerReportingPeriodMaterializer;
import com.codeworkdigital.api.processanalysis.application.ProcessStageInputNature;
import com.codeworkdigital.api.processanalysis.application.ProcessStageOperationType;
import com.codeworkdigital.api.processanalysis.application.ProcessStageProvenance;
import com.codeworkdigital.api.processanalysis.application.ProcessUnderstanding;
import com.codeworkdigital.api.processanalysis.application.ProcessUnderstandingStage;
import com.codeworkdigital.api.processanalysis.application.TechnologyFitAssessmentEvaluator;
import com.codeworkdigital.api.shared.error.ApiExceptionHandler;
import com.codeworkdigital.api.shared.security.ApiSecurityConfiguration;
import com.codeworkdigital.api.shared.web.ApiCorsConfiguration;
import com.codeworkdigital.api.shared.web.ContactRequestBodyLimitFilter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
        classes = ProcessAnalysisApiIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "cwd.admin.username=admin-test",
                "cwd.admin.password=admin-test-password-not-real",
                "cwd.web.allowed-origins=http://localhost:3000",
                "cwd.web.cors-max-age=1h",
                "cwd.web.max-contact-request-bytes=65536",
                "management.health.db.enabled=false",
                "spring.flyway.enabled=false"
        })
class ProcessAnalysisApiIntegrationTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";
    private static final String DISALLOWED_ORIGIN = "http://malicious.example.test";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FakeProcessAnalysisModelClient processAnalysisModelClient;

    @BeforeEach
    void resetFakeClient() {
        processAnalysisModelClient.reset();
    }

    @Test
    void validRequestReturnsStructuredUnderstandingWithoutAuthentication() throws Exception {
        HttpResponse<String> response = post("""
                {
                  "description": "  Recibimos pedidos por WhatsApp, verificamos stock y confirmamos entrega.  ",
                  "locale": "ES"
                }
                """);
        Map<String, Object> body = json(response);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type")).contains("application/json");
        assertThat(response.headers().firstValue("cache-control")).contains("no-store");
        assertThat(body.get("processDescription"))
                .isEqualTo("Recibimos pedidos por WhatsApp, verificamos stock y confirmamos entrega.");
        assertThat(body.get("analysisStatus")).isEqualTo("PROCESS_IDENTIFIED");
        assertThat(body).doesNotContainKey("processIdentified");
        assertThat((List<?>) body.get("observations")).isNotEmpty();
        assertThat((List<?>) body.get("stages")).hasSize(2);
        assertThat((List<?>) body.get("technologyFitAssessments")).hasSize(2);
        assertThat(body.keySet()).containsExactlyInAnyOrder(
                "processDescription",
                "analysisStatus",
                "observations",
                "inferences",
                "validationQuestions",
                "stages",
                "preliminaryAssessment",
                "technologyFitAssessments");
        assertThat(body).doesNotContainKeys(
                "effortEvidence",
                "volumeProjection",
                "effortProjection",
                "sourceKnowledge",
                "derivedResult",
                "materialityAssessment",
                "materialityThreshold",
                "establishedOperationalBurden",
                "derivedEffort",
                "derivation",
                "evidenceBase",
                "knownFacts",
                "fact-volume-per-reporting-period",
                "fact-effort-per-business-item",
                "fact-effort-per-reporting-period",
                "NOT_ESTABLISHED",
                "NO_MATERIAL_JUSTIFICATION_IDENTIFIED",
                "OPPORTUNITY_IDENTIFIED",
                "2400",
                "40 hours",
                "source-process-description",
                "computableProjection",
                "evidenceArtifactIds",
                "businessItemRef",
                "businessItemLabel",
                "composable");
        assertThat(processAnalysisModelClient.invocations).isEqualTo(1);
        assertThat(processAnalysisModelClient.lastCommand.description())
                .isEqualTo("Recibimos pedidos por WhatsApp, verificamos stock y confirmamos entrega.");
        assertThat(response.body()).doesNotContain(
                "openai",
                "prompt",
                "provider",
                "8000 minute/month",
                "2400",
                "40 hours",
                "NOT_ESTABLISHED",
                "NO_MATERIAL_JUSTIFICATION_IDENTIFIED",
                "OPPORTUNITY_IDENTIFIED",
                "materialityAssessment",
                "materialityThreshold",
                "12 minute per month",
                "Deterministically derived effort");
    }

    @Test
    void allowedPreflightReturnsCorsAuthorizationWithoutAuthentication() throws Exception {
        HttpResponse<String> response = options(ALLOWED_ORIGIN, "Content-Type");

        assertThat(response.statusCode()).isBetween(200, 299);
        assertThat(response.headers().firstValue("www-authenticate")).isEmpty();
        assertThat(response.headers().firstValue("access-control-allow-origin")).contains(ALLOWED_ORIGIN);
        assertThat(response.headers().firstValue("access-control-allow-methods")).hasValueSatisfying(value ->
                assertThat(value).contains("POST"));
        assertThat(response.headers().firstValue("access-control-allow-headers")).hasValueSatisfying(value ->
                assertThat(value.toLowerCase()).contains("content-type"));
        assertThat(response.headers().firstValue("access-control-max-age")).contains("3600");
        assertThat(response.headers().firstValue("access-control-allow-credentials")).isEmpty();
        assertThat(response.headers().firstValue("vary")).isPresent();
        assertThat(processAnalysisModelClient.invocations).isZero();
    }

    @Test
    void disallowedPreflightDoesNotAuthorizeCors() throws Exception {
        HttpResponse<String> response = options(DISALLOWED_ORIGIN, "Content-Type");

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.headers().firstValue("access-control-allow-origin")).isEmpty();
        assertThat(processAnalysisModelClient.invocations).isZero();
    }

    @Test
    void allowedOriginPostIncludesExactCorsAuthorization() throws Exception {
        HttpResponse<String> response = postWithOrigin(
                """
                {
                  "description": "Recibimos pedidos por WhatsApp, verificamos stock y confirmamos entrega.",
                  "locale": "ES"
                }
                """,
                ALLOWED_ORIGIN);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("access-control-allow-origin")).contains(ALLOWED_ORIGIN);
        assertThat(response.headers().firstValue("access-control-allow-credentials")).isEmpty();
        assertThat(processAnalysisModelClient.invocations).isEqualTo(1);
    }

    @Test
    void insufficientInformationReturnsSemanticSuccessWithoutTechnologyFit() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.INSUFFICIENT_INFORMATION;

        HttpResponse<String> response = post("""
                {
                  "description": "Queremos usar IA para ser mas eficientes.",
                  "locale": "ES"
                }
                """);
        Map<String, Object> body = json(response);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.get("analysisStatus")).isEqualTo("INSUFFICIENT_INFORMATION");
        assertThat(body).doesNotContainKey("processIdentified");
        assertThat((List<?>) body.get("observations")).isEmpty();
        assertThat((List<?>) body.get("inferences")).isEmpty();
        assertThat((List<?>) body.get("validationQuestions")).isEmpty();
        assertThat((List<?>) body.get("stages")).isEmpty();
        assertThat(body.get("preliminaryAssessment")).isEqualTo("");
        assertThat((List<?>) body.get("technologyFitAssessments")).isEmpty();
        assertThat(processAnalysisModelClient.invocations).isEqualTo(1);
    }

    @Test
    void outOfScopeReturnsSemanticSuccessWithoutGeneralistAnswer() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.OUT_OF_SCOPE;

        HttpResponse<String> response = post("""
                {
                  "description": "Demuestra que sqrt(2) es irracional.",
                  "locale": "EN"
                }
                """);
        Map<String, Object> body = json(response);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.get("analysisStatus")).isEqualTo("OUT_OF_SCOPE");
        assertThat(body).doesNotContainKey("processIdentified");
        assertThat((List<?>) body.get("stages")).isEmpty();
        assertThat((List<?>) body.get("technologyFitAssessments")).isEmpty();
        assertThat(body.get("preliminaryAssessment")).isEqualTo("");
        assertThat(response.body()).doesNotContain("irrational", "proof", "theorem");
        assertThat(processAnalysisModelClient.invocations).isEqualTo(1);
    }

    @Test
    void rejectsBlankDescriptionWithoutCallingModel() throws Exception {
        HttpResponse<String> response = post("""
                {
                  "description": "   ",
                  "locale": "ES"
                }
                """);

        assertValidationError(response, "description", "required");
        assertThat(processAnalysisModelClient.invocations).isZero();
    }

    @Test
    void rejectsDescriptionAboveMaximumWithoutCallingModel() throws Exception {
        HttpResponse<String> response = post("""
                {
                  "description": "%s",
                  "locale": "ES"
                }
                """.formatted("a".repeat(2001)));

        assertValidationError(response, "description", "invalid_length");
        assertThat(processAnalysisModelClient.invocations).isZero();
    }

    @Test
    void rejectsInvalidLocaleWithoutCallingModel() throws Exception {
        HttpResponse<String> response = post("""
                {
                  "description": "Recibimos pedidos y confirmamos stock.",
                  "locale": "PT"
                }
                """);

        assertValidationError(response, "locale", "unsupported_value");
        assertThat(processAnalysisModelClient.invocations).isZero();
    }

    @Test
    void rejectsUnknownJsonFieldBeforeCallingModel() throws Exception {
        HttpResponse<String> response = post("""
                {
                  "description": "Recibimos pedidos y confirmamos stock.",
                  "locale": "ES",
                  "unexpected": true
                }
                """);

        assertProblem(response, 400, "invalid_request", "/api/labs/process-analysis");
        assertThat(processAnalysisModelClient.invocations).isZero();
    }

    @Test
    void unavailableProviderReturns503WithoutLeakingInput() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.UNAVAILABLE;

        HttpResponse<String> response = post("""
                {
                  "description": "Proceso confidencial con datos internos.",
                  "locale": "EN"
                }
                """);

        assertProblem(response, 503, "process_analysis_unavailable", "/api/labs/process-analysis");
        assertThat(response.body()).doesNotContain("Proceso confidencial", "feature_disabled");
        assertThat(processAnalysisModelClient.invocations).isEqualTo(1);
    }

    @Test
    void invalidModelResponseReturns502WithoutLeakingInput() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.INVALID_RESPONSE;

        HttpResponse<String> response = post("""
                {
                  "description": "Proceso reservado para validar errores.",
                  "locale": "IT"
                }
                """);

        assertProblem(response, 502, "invalid_model_response", "/api/labs/process-analysis");
        assertThat(response.body()).doesNotContain("Proceso reservado", "stage_id_duplicate");
        assertThat(processAnalysisModelClient.invocations).isEqualTo(1);
    }

    @Test
    void adminRouteRemainsProtected() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/admin/contact-submissions"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("www-authenticate")).isPresent();
    }

    private void assertValidationError(HttpResponse<String> response, String field, String code) throws Exception {
        assertProblem(response, 400, "validation_failed", "/api/labs/process-analysis");
        Map<String, Object> body = json(response);
        assertThat((List<?>) body.get("errors")).anySatisfy(error -> {
            Map<?, ?> entry = (Map<?, ?>) error;
            assertThat(entry.get("field")).isEqualTo(field);
            assertThat(entry.get("code")).isEqualTo(code);
        });
        assertThat(response.body()).doesNotContain("stackTrace", "exception", "rejectedValue");
    }

    private void assertProblem(HttpResponse<String> response, int status, String code, String instance) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("content-type")).contains("application/problem+json");
        Map<String, Object> body = json(response);
        assertThat(body.get("type")).isEqualTo("urn:codeworkdigital:problem:" + code);
        assertThat(body.get("status")).isEqualTo(status);
        assertThat(body.get("instance")).isEqualTo(instance);
        assertThat(body.get("code")).isEqualTo(code);
    }

    private HttpResponse<String> post(String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/labs/process-analysis"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postWithOrigin(String body, String origin) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/labs/process-analysis"))
                .header("Origin", origin)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> options(String origin, String requestHeaders) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/labs/process-analysis"))
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", requestHeaders)
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> json(HttpResponse<String> response) throws Exception {
        return objectMapper.readValue(response.body(), Map.class);
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.health.DataSourceHealthContributorAutoConfiguration"
    })
    @Import({
            ProcessAnalysisController.class,
            ProcessAnalysisApplicationService.class,
            TechnologyFitAssessmentEvaluator.class,
            ApiSecurityConfiguration.class,
            ApiCorsConfiguration.class,
            ApiExceptionHandler.class,
            ContactRequestBodyLimitFilter.class,
            ProcessEffortEvidenceProjectionMapper.class,
            ProcessEffortPerReportingPeriodMaterializer.class,
            ProcessEffortMaterialityAssessmentEvaluator.class,
            TestConfig.class
    })
    static class TestApplication {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {

        @Bean
        FakeProcessAnalysisModelClient fakeProcessAnalysisModelClient() {
            return new FakeProcessAnalysisModelClient();
        }
    }

    static class FakeProcessAnalysisModelClient implements ProcessAnalysisModelClient {

        enum Mode {
            SUCCESS,
            INSUFFICIENT_INFORMATION,
            OUT_OF_SCOPE,
            UNAVAILABLE,
            INVALID_RESPONSE
        }

        private int invocations;
        private AnalyzeProcessDescriptionCommand lastCommand;
        private Mode mode = Mode.SUCCESS;

        @Override
        public ProcessAnalysisModelResult analyze(AnalyzeProcessDescriptionCommand command) {
            invocations++;
            lastCommand = command;
            ProcessUnderstanding understanding = switch (mode) {
                case SUCCESS -> new ProcessUnderstanding(
                        command.description(),
                        List.of("El proceso recibe pedidos por mensajeria."),
                        List.of("Puede existir una verificacion manual antes de confirmar."),
                        List.of("Donde vive el stock canonico?"),
                        List.of(
                                new ProcessUnderstandingStage(
                                        "receive-request",
                                        "Recepcion del pedido",
                                        "Se recibe un pedido desde mensajeria.",
                                        ProcessStageProvenance.OBSERVED,
                                        ProcessStageOperationType.RECEIVE,
                                        ProcessStageInputNature.UNSTRUCTURED),
                                new ProcessUnderstandingStage(
                                        "confirm-stock",
                                        "Confirmacion de stock",
                                        "Se revisa disponibilidad antes de responder.",
                                        ProcessStageProvenance.INFERRED,
                                        ProcessStageOperationType.VALIDATE,
                                        ProcessStageInputNature.MIXED)),
                        "Este entendimiento es preliminar y depende de validar como se consulta stock y que excepciones existen.");
                case INSUFFICIENT_INFORMATION -> new ProcessUnderstanding(
                        command.description(),
                        ProcessAnalysisStatus.INSUFFICIENT_INFORMATION,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        "");
                case OUT_OF_SCOPE -> new ProcessUnderstanding(
                        command.description(),
                        ProcessAnalysisStatus.OUT_OF_SCOPE,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        "");
                case UNAVAILABLE -> throw new ProcessAnalysisUnavailableException("simulated_unavailable");
                case INVALID_RESPONSE -> throw new InvalidProcessAnalysisModelResponseException("simulated_invalid_response");
            };
            return new ProcessAnalysisModelResult(understanding, exactEvidence());
        }

        void reset() {
            invocations = 0;
            lastCommand = null;
            mode = Mode.SUCCESS;
        }

        private ProcessEffortEvidence exactEvidence() {
            return new ProcessEffortEvidence(
                    new ProcessEffortEvidenceQuantity(
                            ProcessEffortEvidenceQuantityStatus.EXACT,
                            new BigDecimal("4"),
                            null,
                            null,
                            "item-1",
                            "pedido",
                            com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit.MONTH,
                            null,
                            "4 pedidos por mes",
                            null),
                    new ProcessEffortEvidenceQuantity(
                            ProcessEffortEvidenceQuantityStatus.EXACT,
                            new BigDecimal("3"),
                            null,
                            null,
                            "item-1",
                            "pedido",
                            null,
                            com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit.MINUTE,
                            "3 minutos por pedido",
                            null));
        }
    }
}
