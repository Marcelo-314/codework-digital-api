package com.codeworkdigital.api.processanalysis.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeworkdigital.api.processanalysis.application.AnalyzeProcessDescriptionCommand;
import com.codeworkdigital.api.processanalysis.application.InvalidProcessAnalysisModelResponseException;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisApplicationService;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisModelClient;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisModelResult;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisUnavailableException;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisStatus;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContinuation;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContinuationId;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContinuationIssuer;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContinuationRepository;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContinuationResolutionService;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationAnswerMaterializer;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationResolver;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortEstablishedKnowledgeComposer;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortEvidence;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortEvidenceProjectionMapper;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortEvidenceQuantity;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortEvidenceQuantityStatus;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortMaterialityAssessmentEvaluator;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortMaterialityEvidenceGapIdentifier;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

    @Autowired
    private RecordingProcessEffortClarificationContinuationRepository continuationRepository;

    @BeforeEach
    void resetFakeClient() {
        processAnalysisModelClient.reset();
        continuationRepository.reset();
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
        assertThat(body.get("clarificationId")).isNull();
        assertThat((List<?>) body.get("clarificationQuestions")).isEmpty();
        assertInitialReasoning4000By2(body);
        assertThat(body).doesNotContainKey("understanding");
        assertThat(body.keySet()).containsExactlyInAnyOrder(
                "processDescription",
                "analysisStatus",
                "observations",
                "inferences",
                "validationQuestions",
                "stages",
                "preliminaryAssessment",
                "technologyFitAssessments",
                "clarificationId",
                "clarificationQuestions",
                "reasoning");
        assertThat(body).doesNotContainKeys(
                "effortEvidence",
                "volumeProjection",
                "effortProjection",
                "sourceKnowledge",
                "derivedResult",
                "materialityAssessment",
                "materialityThreshold",
                "materialityEvidenceGaps",
                "ProcessEffortMaterialityEvidenceGap",
                "ProcessEffortMaterialityEvidenceGapKind",
                "ProcessEvidenceGap",
                "clarification question",
                "decisionAffected",
                "SELF_REPORTED",
                "establishedOperationalBurden",
                "derivedEffort",
                "derivation",
                "evidenceBase",
                "knownFacts",
                "fact-volume-per-reporting-period",
                "fact-effort-per-business-item",
                "fact-effort-per-reporting-period",
                "NOT_ESTABLISHED",
                "40 hours",
                sourceDescriptionArtifactId(),
                "computableProjection",
                "evidenceArtifactIds",
                "businessItemRef",
                "businessItemLabel",
                "composable");
        assertThat(processAnalysisModelClient.invocations).isEqualTo(1);
        assertThat(continuationRepository.saved).isEmpty();
        assertThat(processAnalysisModelClient.lastCommand.description())
                .isEqualTo("Recibimos pedidos por WhatsApp, verificamos stock y confirmamos entrega.");
        assertThat(response.body()).doesNotContain(
                "openai",
                "prompt",
                "provider",
                "8000 minute/month",
                "40 hours",
                "NOT_ESTABLISHED",
                "materialityAssessment",
                "materialityThreshold",
                "materialityEvidenceGaps",
                "ProcessEffortMaterialityEvidenceGap",
                "ProcessEffortMaterialityEvidenceGapKind",
                "decisionAffected",
                "SELF_REPORTED",
                "12 minute per month",
                "Deterministically derived effort");
    }

    @Test
    void absentVolumeReturnsPublicVolumeClarificationQuestion() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.VOLUME_ABSENT;

        Map<String, Object> body = json(postWithLocale("EN"));

        assertClarificationQuestions(body,
                Map.of(
                        "code", "VOLUME_PER_REPORTING_PERIOD",
                        "question", "What monthly quantity do you use as the reference volume for this process?"));
        assertNonNullClarificationId(body);
        assertPartialReasoning(body, "EFFORT_PER_BUSINESS_ITEM", "3", "MINUTE_PER_BUSINESS_ITEM");
        assertThat(continuationRepository.saved).hasSize(1);
        assertThat(body).containsKey("validationQuestions");
        assertThat(body).doesNotContainKey("understanding");
        assertNoInternalAnalysisFieldsLeak(body);
        assertNoInternalAnalysisTextLeaks(lastResponseBody);
        assertThat(processAnalysisModelClient.invocations).isEqualTo(1);
    }

    @Test
    void continuationPersistenceFailureFailsRequestClosed() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.VOLUME_ABSENT;
        continuationRepository.failure = new IllegalStateException("simulated continuation persistence failure");

        HttpResponse<String> response = postWithLocale("EN");

        assertThat(response.statusCode()).isGreaterThanOrEqualTo(300);
        assertThat(response.body()).doesNotContain("clarificationQuestions", "clarificationId");
        assertThat(continuationRepository.saved).isEmpty();
        assertThat(processAnalysisModelClient.invocations).isEqualTo(1);
    }

    @Test
    void absentEffortReturnsPublicEffortClarificationQuestion() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.EFFORT_ABSENT;

        Map<String, Object> body = json(postWithLocale("EN"));

        assertClarificationQuestions(body,
                Map.of(
                        "code", "EFFORT_PER_BUSINESS_ITEM",
                        "question", "How many minutes of effort per processed business item do you use as the reference value?"));
        assertNonNullClarificationId(body);
        assertPartialReasoning(body, "VOLUME_PER_REPORTING_PERIOD", "4000", "BUSINESS_ITEM_PER_MONTH");
        assertThat(continuationRepository.saved).hasSize(1);
        assertNoInternalAnalysisFieldsLeak(body);
        assertThat(processAnalysisModelClient.invocations).isEqualTo(1);
    }

    @Test
    void bothAbsentReturnPublicQuestionsInDeterministicVolumeThenEffortOrder() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.BOTH_ABSENT;

        Map<String, Object> body = json(postWithLocale("EN"));

        assertClarificationQuestions(body,
                Map.of(
                        "code", "VOLUME_PER_REPORTING_PERIOD",
                        "question", "What monthly quantity do you use as the reference volume for this process?"),
                Map.of(
                        "code", "EFFORT_PER_BUSINESS_ITEM",
                        "question", "How many minutes of effort per processed business item do you use as the reference value?"));
        assertNonNullClarificationId(body);
        assertThat(continuationRepository.saved).hasSize(1);
        assertThat(processAnalysisModelClient.invocations).isEqualTo(1);
    }

    @Test
    void publicClarificationQuestionsUseLocalizedTextAndStableCodes() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.BOTH_ABSENT;

        Map<String, Object> spanish = json(postWithLocale("ES"));
        Map<String, Object> italian = json(postWithLocale("IT"));
        Map<String, Object> english = json(postWithLocale("EN"));

        assertClarificationQuestions(spanish,
                Map.of(
                        "code", "VOLUME_PER_REPORTING_PERIOD",
                        "question", "¿Qué cantidad mensual usas como volumen de referencia para este proceso?"),
                Map.of(
                        "code", "EFFORT_PER_BUSINESS_ITEM",
                        "question", "¿Cuántos minutos de esfuerzo por ítem de negocio procesado usas como valor de referencia?"));
        assertClarificationQuestions(italian,
                Map.of(
                        "code", "VOLUME_PER_REPORTING_PERIOD",
                        "question", "Quale quantità mensile usi come volume di riferimento per questo processo?"),
                Map.of(
                        "code", "EFFORT_PER_BUSINESS_ITEM",
                        "question", "Quanti minuti di lavoro per elemento di business processato usi come valore di riferimento?"));
        assertClarificationQuestions(english,
                Map.of(
                        "code", "VOLUME_PER_REPORTING_PERIOD",
                        "question", "What monthly quantity do you use as the reference volume for this process?"),
                Map.of(
                        "code", "EFFORT_PER_BUSINESS_ITEM",
                        "question", "How many minutes of effort per processed business item do you use as the reference value?"));
        assertNonNullClarificationId(spanish);
        assertNonNullClarificationId(italian);
        assertNonNullClarificationId(english);
        assertThat(continuationRepository.saved).hasSize(3);
        assertThat(processAnalysisModelClient.invocations).isEqualTo(3);
    }

    @Test
    void unsupportedNonAbsentCausesDoNotCreatePublicClarificationQuestions() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.UNSUPPORTED_NON_ABSENT;

        Map<String, Object> body = json(postWithLocale("EN"));

        assertThat(body.get("clarificationQuestions")).isEqualTo(List.of());
        assertThat(body.get("clarificationId")).isNull();
        assertThat(continuationRepository.saved).isEmpty();
        assertThat(processAnalysisModelClient.invocations).isEqualTo(1);
    }

    @Test
    void nonActionableAbsentEvidenceReturnsEmptyPublicClarificationQuestions() throws Exception {
        for (FakeProcessAnalysisModelClient.Mode mode : List.of(
                FakeProcessAnalysisModelClient.Mode.VOLUME_ABSENT_MISMATCHED_REF,
                FakeProcessAnalysisModelClient.Mode.VOLUME_ABSENT_UNSUPPORTED_EFFORT,
                FakeProcessAnalysisModelClient.Mode.BOTH_ABSENT_BLANK_REF,
                FakeProcessAnalysisModelClient.Mode.VOLUME_ABSENT_MISSING_LABEL)) {
            processAnalysisModelClient.mode = mode;

            Map<String, Object> body = json(postWithLocale("EN"));

            assertThat(body.get("clarificationQuestions")).isEqualTo(List.of());
            assertThat(body.get("clarificationId")).isNull();
            assertNoInternalAnalysisFieldsLeak(body);
        }
        assertThat(continuationRepository.saved).isEmpty();
        assertThat(processAnalysisModelClient.invocations).isEqualTo(4);
    }

    @Test
    void publicClarificationQuestionRootShapeRemainsStableForActionableCases() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.BOTH_ABSENT;

        Map<String, Object> body = json(postWithLocale("EN"));

        assertThat(body.keySet()).containsExactlyInAnyOrder(
                "processDescription",
                "analysisStatus",
                "observations",
                "inferences",
                "validationQuestions",
                "stages",
                "preliminaryAssessment",
                "technologyFitAssessments",
                "clarificationId",
                "clarificationQuestions",
                "reasoning");
        assertEmptyReasoning(body);
        assertClarificationQuestions(body,
                Map.of(
                        "code", "VOLUME_PER_REPORTING_PERIOD",
                        "question", "What monthly quantity do you use as the reference volume for this process?"),
                Map.of(
                        "code", "EFFORT_PER_BUSINESS_ITEM",
                        "question", "How many minutes of effort per processed business item do you use as the reference value?"));
        String clarificationId = assertNonNullClarificationId(body);
        assertThat(clarificationId).isNotEqualTo("item-1");
        assertThat(lastResponseBody).doesNotContain(
                "businessItemRef",
                "businessItemLabel",
                "context",
                "createdAt",
                "expiresAt",
                "sourceKnowledge",
                "materialityAssessment",
                "item-1");
        assertThat(processAnalysisModelClient.invocations).isEqualTo(1);
    }

    @Test
    void belowThresholdInitialResultExposesPublicBelowThresholdDecision() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.SUCCESS_BELOW_THRESHOLD;

        Map<String, Object> body = json(postWithLocale("EN"));

        Map<?, ?> decision = decision(body);
        assertThat(decision.get("comparison")).isEqualTo("BELOW_THRESHOLD");
        assertThat(decision.get("outcome")).isEqualTo("NO_MATERIAL_JUSTIFICATION_IDENTIFIED");
        Map<?, ?> threshold = (Map<?, ?>) decision.get("threshold");
        assertThat(new BigDecimal(threshold.get("magnitude").toString())).isEqualByComparingTo("2400");
        assertThat(threshold.get("unit")).isEqualTo("MINUTE_PER_MONTH");
    }

    @Test
    void volumeClarificationAnswerReturnsNarrowResolutionWithoutAuthenticationOrSecondModelCall() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.VOLUME_ABSENT;
        String clarificationId = assertNonNullClarificationId(json(postWithLocale("EN")));

        HttpResponse<String> response = answer(clarificationId, """
                {
                  "answers": [
                    {
                      "code": "VOLUME_PER_REPORTING_PERIOD",
                      "value": 4000
                    }
                  ]
                }
                """);
        Map<String, Object> body = json(response);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("www-authenticate")).isEmpty();
        assertThat(body.keySet()).containsExactlyInAnyOrder(
                "clarificationId",
                "operationalBurden",
                "materialityOutcome",
                "reasoning");
        assertThat(body.get("clarificationId")).isEqualTo(clarificationId);
        Map<?, ?> burden = (Map<?, ?>) body.get("operationalBurden");
        assertThat(new BigDecimal(burden.get("magnitude").toString())).isEqualByComparingTo("12000");
        assertThat(burden.get("unit")).isEqualTo("MINUTE_PER_MONTH");
        assertThat(body.get("materialityOutcome")).isEqualTo("OPPORTUNITY_IDENTIFIED");
        assertReasoningInputs(body,
                input("VOLUME_PER_REPORTING_PERIOD", "4000", "BUSINESS_ITEM_PER_MONTH", "CLARIFICATION_ANSWER"),
                input("EFFORT_PER_BUSINESS_ITEM", "3", "MINUTE_PER_BUSINESS_ITEM", "PROCESS_DESCRIPTION"));
        assertCalculation(body, "12000");
        assertDecision(body, "AT_OR_ABOVE_THRESHOLD", "OPPORTUNITY_IDENTIFIED");
        assertThat(response.body()).doesNotContain(
                "NOT_ESTABLISHED",
                "businessItemRef",
                "businessItemLabel",
                "SOURCE_STATED",
                "DETERMINISTICALLY_DERIVED",
                "premise",
                "sourceKnowledge",
                "knownFacts",
                "evidence",
                "item-1");
        assertThat(processAnalysisModelClient.invocations).isEqualTo(1);
        assertThat(continuationRepository.findById(new ProcessEffortClarificationContinuationId(
                        UUID.fromString(clarificationId))).orElseThrow().resolvedAt())
                .contains(Instant.parse("2026-08-21T12:00:00Z"));
    }

    @Test
    void effortClarificationAnswerReturnsNarrowResolution() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.EFFORT_ABSENT;
        String clarificationId = assertNonNullClarificationId(json(postWithLocale("EN")));

        Map<String, Object> body = json(answer(clarificationId, """
                {
                  "answers": [
                    {
                      "code": "EFFORT_PER_BUSINESS_ITEM",
                      "value": 2
                    }
                  ]
                }
                """));

        Map<?, ?> burden = (Map<?, ?>) body.get("operationalBurden");
        assertThat(new BigDecimal(burden.get("magnitude").toString())).isEqualByComparingTo("8000");
        assertThat(burden.get("unit")).isEqualTo("MINUTE_PER_MONTH");
        assertThat(body.get("materialityOutcome")).isEqualTo("OPPORTUNITY_IDENTIFIED");
        assertReasoningInputs(body,
                input("VOLUME_PER_REPORTING_PERIOD", "4000", "BUSINESS_ITEM_PER_MONTH", "PROCESS_DESCRIPTION"),
                input("EFFORT_PER_BUSINESS_ITEM", "2", "MINUTE_PER_BUSINESS_ITEM", "CLARIFICATION_ANSWER"));
        assertCalculation(body, "8000");
        assertDecision(body, "AT_OR_ABOVE_THRESHOLD", "OPPORTUNITY_IDENTIFIED");
    }

    @Test
    void bothClarificationAnswersReturnExpectedMaterialityOutcome() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.BOTH_ABSENT;
        String clarificationId = assertNonNullClarificationId(json(postWithLocale("EN")));

        Map<String, Object> body = json(answer(clarificationId, """
                {
                  "answers": [
                    {
                      "code": "VOLUME_PER_REPORTING_PERIOD",
                      "value": 4
                    },
                    {
                      "code": "EFFORT_PER_BUSINESS_ITEM",
                      "value": 2
                    }
                  ]
                }
                """));

        Map<?, ?> burden = (Map<?, ?>) body.get("operationalBurden");
        assertThat(new BigDecimal(burden.get("magnitude").toString())).isEqualByComparingTo("8");
        assertThat(body.get("materialityOutcome"))
                .isEqualTo("NO_MATERIAL_JUSTIFICATION_IDENTIFIED");
        assertReasoningInputs(body,
                input("VOLUME_PER_REPORTING_PERIOD", "4", "BUSINESS_ITEM_PER_MONTH", "CLARIFICATION_ANSWER"),
                input("EFFORT_PER_BUSINESS_ITEM", "2", "MINUTE_PER_BUSINESS_ITEM", "CLARIFICATION_ANSWER"));
        assertCalculation(body, "8");
        assertDecision(body, "BELOW_THRESHOLD", "NO_MATERIAL_JUSTIFICATION_IDENTIFIED");
    }

    @Test
    void clarificationReasoningRepresentsZeroAndDecimalResults() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.BOTH_ABSENT;
        String zeroClarificationId = assertNonNullClarificationId(json(postWithLocale("EN")));

        Map<String, Object> zero = json(answer(zeroClarificationId, """
                {
                  "answers": [
                    {
                      "code": "VOLUME_PER_REPORTING_PERIOD",
                      "value": 0
                    },
                    {
                      "code": "EFFORT_PER_BUSINESS_ITEM",
                      "value": 2
                    }
                  ]
                }
                """));
        assertCalculation(zero, "0");
        assertDecision(zero, "BELOW_THRESHOLD", "NO_MATERIAL_JUSTIFICATION_IDENTIFIED");

        String decimalClarificationId = assertNonNullClarificationId(json(postWithLocale("EN")));
        Map<String, Object> decimal = json(answer(decimalClarificationId, """
                {
                  "answers": [
                    {
                      "code": "VOLUME_PER_REPORTING_PERIOD",
                      "value": 4000.25
                    },
                    {
                      "code": "EFFORT_PER_BUSINESS_ITEM",
                      "value": 2.5
                    }
                  ]
                }
                """));
        assertCalculation(decimal, "10000.625");
        assertReasoningInputs(decimal,
                input("VOLUME_PER_REPORTING_PERIOD", "4000.25", "BUSINESS_ITEM_PER_MONTH", "CLARIFICATION_ANSWER"),
                input("EFFORT_PER_BUSINESS_ITEM", "2.5", "MINUTE_PER_BUSINESS_ITEM", "CLARIFICATION_ANSWER"));
    }

    @Test
    void publicReasoningDoesNotLeakInternalGraphFieldsOrClassifySourcesFromStatements() throws Exception {
        HttpResponse<String> initial = postWithLocale("EN");
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.BOTH_ABSENT;
        String clarificationId = assertNonNullClarificationId(json(postWithLocale("EN")));
        HttpResponse<String> clarified = answer(clarificationId, """
                {
                  "answers": [
                    {
                      "code": "VOLUME_PER_REPORTING_PERIOD",
                      "value": 4000
                    },
                    {
                      "code": "EFFORT_PER_BUSINESS_ITEM",
                      "value": 2
                    }
                  ]
                }
                """);

        assertNoPublicReasoningLeak(initial.body());
        assertNoPublicReasoningLeak(clarified.body());
        assertThat(clarified.body()).doesNotContain("NOT_ESTABLISHED");

        String projector = Files.readString(Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/application/ProcessEffortReasoningProjector.java"));
        assertThat(projector).doesNotContain(".statement()");

        for (String apiFile : List.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/api/ProcessAnalysisResponse.java",
                "src/main/java/com/codeworkdigital/api/processanalysis/api/ProcessEffortClarificationResolutionResponse.java",
                "src/main/java/com/codeworkdigital/api/processanalysis/api/ProcessEffortReasoningResponse.java",
                "src/main/java/com/codeworkdigital/api/processanalysis/api/ProcessEffortMaterialityOutcomeResponse.java")) {
            assertThat(Files.readString(Path.of(apiFile))).doesNotContain(
                    sourceDescriptionArtifactId(),
                    volumeClarificationArtifactId(),
                    effortClarificationArtifactId());
        }
        assertThat(Files.readString(Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/api/ProcessEffortClarificationResolutionResponse.java")))
                .doesNotContain("ProcessEffortMaterialityAssessmentStatus materialityOutcome");
    }

    @Test
    void malformedClarificationIdReturnsBadRequestProblem() throws Exception {
        HttpResponse<String> response = answer("not-a-uuid", """
                {
                  "answers": [
                    {
                      "code": "VOLUME_PER_REPORTING_PERIOD",
                      "value": 4000
                    }
                  ]
                }
                """);

        assertProblem(response, 400, "validation_failed",
                "/api/labs/process-analysis/clarifications/not-a-uuid/answers");
    }

    @Test
    void unsupportedAnswerCodeReturnsBadRequestWithoutConsumingContinuation() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.VOLUME_ABSENT;
        String clarificationId = assertNonNullClarificationId(json(postWithLocale("EN")));

        HttpResponse<String> response = answer(clarificationId, """
                {
                  "answers": [
                    {
                      "code": "QUESTION_TEXT_IS_NOT_A_CODE",
                      "value": 4000
                    }
                  ]
                }
                """);

        assertProblem(response, 400, "validation_failed", answerPath(clarificationId));
        assertContinuationRetryable(clarificationId);
    }

    @Test
    void missingClarificationReturnsNotFoundProblem() throws Exception {
        String clarificationId = UUID.randomUUID().toString();

        HttpResponse<String> response = answer(clarificationId, """
                {
                  "answers": [
                    {
                      "code": "VOLUME_PER_REPORTING_PERIOD",
                      "value": 4000
                    }
                  ]
                }
                """);

        assertProblem(response, 404, "clarification_not_found", answerPath(clarificationId));
    }

    @Test
    void expiredClarificationReturnsGoneProblem() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.VOLUME_ABSENT;
        String clarificationId = assertNonNullClarificationId(json(postWithLocale("EN")));
        continuationRepository.expire(clarificationId);

        HttpResponse<String> response = answer(clarificationId, """
                {
                  "answers": [
                    {
                      "code": "VOLUME_PER_REPORTING_PERIOD",
                      "value": 4000
                    }
                  ]
                }
                """);

        assertProblem(response, 410, "clarification_expired", answerPath(clarificationId));
        assertContinuationRetryable(clarificationId);
    }

    @Test
    void alreadyResolvedClarificationReturnsConflictProblem() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.VOLUME_ABSENT;
        String clarificationId = assertNonNullClarificationId(json(postWithLocale("EN")));
        continuationRepository.resolve(clarificationId);

        HttpResponse<String> response = answer(clarificationId, """
                {
                  "answers": [
                    {
                      "code": "VOLUME_PER_REPORTING_PERIOD",
                      "value": 4000
                    }
                  ]
                }
                """);

        assertProblem(response, 409, "clarification_already_resolved", answerPath(clarificationId));
    }

    @Test
    void lifecycleRaceReturnsConflictAfterSuccessfulCalculation() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.VOLUME_ABSENT;
        String clarificationId = assertNonNullClarificationId(json(postWithLocale("EN")));
        continuationRepository.markResult = false;

        HttpResponse<String> response = answer(clarificationId, """
                {
                  "answers": [
                    {
                      "code": "VOLUME_PER_REPORTING_PERIOD",
                      "value": 4000
                    }
                  ]
                }
                """);

        assertProblem(response, 409, "clarification_lifecycle_conflict", answerPath(clarificationId));
        assertContinuationRetryable(clarificationId);
    }

    @Test
    void invalidAnswerPayloadDoesNotConsumeContinuationAndCanBeRetried() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.BOTH_ABSENT;
        String clarificationId = assertNonNullClarificationId(json(postWithLocale("EN")));

        assertProblem(answer(clarificationId, """
                {
                  "answers": [
                    {
                      "code": "VOLUME_PER_REPORTING_PERIOD",
                      "value": 4000
                    }
                  ]
                }
                """), 400, "validation_failed", answerPath(clarificationId));
        assertContinuationRetryable(clarificationId);

        HttpResponse<String> retry = answer(clarificationId, """
                {
                  "answers": [
                    {
                      "code": "VOLUME_PER_REPORTING_PERIOD",
                      "value": 4000
                    },
                    {
                      "code": "EFFORT_PER_BUSINESS_ITEM",
                      "value": 2
                    }
                  ]
                }
                """);
        assertThat(retry.statusCode()).isEqualTo(200);
    }

    @Test
    void duplicateExtraAndNegativeAnswerPayloadsReturnBadRequestWithoutInternalLeaks() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.VOLUME_ABSENT;
        String duplicateId = assertNonNullClarificationId(json(postWithLocale("EN")));
        assertProblem(answer(duplicateId, """
                {
                  "answers": [
                    {
                      "code": "VOLUME_PER_REPORTING_PERIOD",
                      "value": 4000
                    },
                    {
                      "code": "VOLUME_PER_REPORTING_PERIOD",
                      "value": 5000
                    }
                  ]
                }
                """), 400, "validation_failed", answerPath(duplicateId));
        assertContinuationRetryable(duplicateId);

        String extraId = assertNonNullClarificationId(json(postWithLocale("EN")));
        assertProblem(answer(extraId, """
                {
                  "answers": [
                    {
                      "code": "VOLUME_PER_REPORTING_PERIOD",
                      "value": 4000
                    },
                    {
                      "code": "EFFORT_PER_BUSINESS_ITEM",
                      "value": 2
                    }
                  ]
                }
                """), 400, "validation_failed", answerPath(extraId));
        assertContinuationRetryable(extraId);

        String negativeId = assertNonNullClarificationId(json(postWithLocale("EN")));
        HttpResponse<String> negative = answer(negativeId, """
                {
                  "answers": [
                    {
                      "code": "VOLUME_PER_REPORTING_PERIOD",
                      "value": -1
                    }
                  ]
                }
                """);
        assertProblem(negative, 400, "validation_failed", answerPath(negativeId));
        assertContinuationRetryable(negativeId);
        assertThat(negative.body()).doesNotContain(
                "businessItemRef",
                "businessItemLabel",
                "knownFacts",
                sourceDescriptionArtifactId(),
                "item-1");
    }

    @Test
    void answerPayloadRejectsNonPublicFields() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.VOLUME_ABSENT;
        String clarificationId = assertNonNullClarificationId(json(postWithLocale("EN")));

        HttpResponse<String> response = answer(clarificationId, """
                {
                  "answers": [
                    {
                      "code": "VOLUME_PER_REPORTING_PERIOD",
                      "value": 4000,
                      "unit": "MINUTE_PER_MONTH"
                    }
                  ]
                }
                """);

        assertProblem(response, 400, "invalid_request", answerPath(clarificationId));
        assertContinuationRetryable(clarificationId);
    }

    @Test
    void secondAnswerRequestForSameClarificationDoesNotResolveAgain() throws Exception {
        processAnalysisModelClient.mode = FakeProcessAnalysisModelClient.Mode.VOLUME_ABSENT;
        String clarificationId = assertNonNullClarificationId(json(postWithLocale("EN")));

        assertThat(answer(clarificationId, """
                {
                  "answers": [
                    {
                      "code": "VOLUME_PER_REPORTING_PERIOD",
                      "value": 4000
                    }
                  ]
                }
                """).statusCode()).isEqualTo(200);

        HttpResponse<String> second = answer(clarificationId, """
                {
                  "answers": [
                    {
                      "code": "VOLUME_PER_REPORTING_PERIOD",
                      "value": 4000
                    }
                  ]
                }
                """);
        assertProblem(second, 409, "clarification_already_resolved", answerPath(clarificationId));
        assertThat(continuationRepository.markCalls).isEqualTo(1);
    }

    @Test
    void clarificationAnswerPreflightReturnsCorsAuthorizationWithoutAuthentication() throws Exception {
        HttpResponse<String> response = options(
                "/api/labs/process-analysis/clarifications/%s/answers".formatted(UUID.randomUUID()),
                ALLOWED_ORIGIN,
                "Content-Type");

        assertThat(response.statusCode()).isBetween(200, 299);
        assertThat(response.headers().firstValue("www-authenticate")).isEmpty();
        assertThat(response.headers().firstValue("access-control-allow-origin")).contains(ALLOWED_ORIGIN);
        assertThat(response.headers().firstValue("access-control-allow-methods")).hasValueSatisfying(value ->
                assertThat(value).contains("POST"));
        assertThat(response.headers().firstValue("access-control-allow-headers")).hasValueSatisfying(value ->
                assertThat(value.toLowerCase()).contains("content-type"));
        assertThat(response.headers().firstValue("access-control-max-age")).contains("3600");
    }

    @Test
    void clarificationAnswerDisallowedPreflightDoesNotAuthorizeCors() throws Exception {
        HttpResponse<String> response = options(
                "/api/labs/process-analysis/clarifications/%s/answers".formatted(UUID.randomUUID()),
                DISALLOWED_ORIGIN,
                "Content-Type");

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.headers().firstValue("access-control-allow-origin")).isEmpty();
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
        assertThat(body.get("clarificationId")).isNull();
        assertThat(body.get("clarificationQuestions")).isEqualTo(List.of());
        assertEmptyReasoning(body);
        assertThat(continuationRepository.saved).isEmpty();
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
        assertThat(body.get("clarificationId")).isNull();
        assertThat(body.get("clarificationQuestions")).isEqualTo(List.of());
        assertThat(body.get("preliminaryAssessment")).isEqualTo("");
        assertEmptyReasoning(body);
        assertThat(response.body()).doesNotContain("irrational", "proof", "theorem");
        assertThat(continuationRepository.saved).isEmpty();
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

    @SafeVarargs
    private void assertClarificationQuestions(
            Map<String, Object> body,
            Map<String, String>... expectedQuestions) {
        assertThat(body.get("clarificationQuestions")).isInstanceOf(List.class);
        List<?> questions = (List<?>) body.get("clarificationQuestions");
        assertThat(questions).hasSize(expectedQuestions.length);
        assertThat(body.get("clarificationId") == null).isEqualTo(questions.isEmpty());
        for (int index = 0; index < expectedQuestions.length; index++) {
            assertThat(questions.get(index)).isInstanceOf(Map.class);
            Map<?, ?> question = (Map<?, ?>) questions.get(index);
            assertThat(question.keySet().stream().map(Object::toString).toList())
                    .containsExactlyInAnyOrder("code", "question");
            assertThat(question.get("code")).isEqualTo(expectedQuestions[index].get("code"));
            assertThat(question.get("question")).isEqualTo(expectedQuestions[index].get("question"));
        }
    }

    private String assertNonNullClarificationId(Map<String, Object> body) {
        assertThat(body.get("clarificationId")).isInstanceOf(String.class);
        String clarificationId = (String) body.get("clarificationId");
        assertThat(UUID.fromString(clarificationId).toString()).isEqualTo(clarificationId);
        return clarificationId;
    }

    private void assertInitialReasoning4000By2(Map<String, Object> body) {
        assertReasoningInputs(body,
                input("VOLUME_PER_REPORTING_PERIOD", "4000", "BUSINESS_ITEM_PER_MONTH", "PROCESS_DESCRIPTION"),
                input("EFFORT_PER_BUSINESS_ITEM", "2", "MINUTE_PER_BUSINESS_ITEM", "PROCESS_DESCRIPTION"));
        assertCalculation(body, "8000");
        Map<?, ?> decision = decision(body);
        assertThat(decision.get("criterion")).isEqualTo("LAB_OPERATIONAL_BURDEN_THRESHOLD");
        Map<?, ?> threshold = (Map<?, ?>) decision.get("threshold");
        assertThat(new BigDecimal(threshold.get("magnitude").toString())).isEqualByComparingTo("2400");
        assertThat(threshold.get("unit")).isEqualTo("MINUTE_PER_MONTH");
        assertThat(decision.get("comparison")).isEqualTo("AT_OR_ABOVE_THRESHOLD");
        assertThat(decision.get("outcome")).isEqualTo("OPPORTUNITY_IDENTIFIED");
    }

    @SafeVarargs
    private void assertReasoningInputs(Map<String, Object> body, Map<String, String>... expectedInputs) {
        Map<?, ?> reasoning = reasoning(body);
        assertThat(reasoning.keySet().stream().map(Object::toString).toList())
                .containsExactlyInAnyOrder("establishedInputs", "calculation", "decision");
        List<?> inputs = (List<?>) reasoning.get("establishedInputs");
        assertThat(inputs).hasSize(expectedInputs.length);
        for (int index = 0; index < expectedInputs.length; index++) {
            Map<?, ?> actual = (Map<?, ?>) inputs.get(index);
            Map<String, String> expected = expectedInputs[index];
            assertThat(actual.keySet().stream().map(Object::toString).toList())
                    .containsExactlyInAnyOrder("code", "magnitude", "unit", "source");
            assertThat(actual.get("code")).isEqualTo(expected.get("code"));
            assertThat(new BigDecimal(actual.get("magnitude").toString()))
                    .isEqualByComparingTo(expected.get("magnitude"));
            assertThat(actual.get("unit")).isEqualTo(expected.get("unit"));
            assertThat(actual.get("source")).isEqualTo(expected.get("source"));
        }
    }

    private Map<String, String> input(String code, String magnitude, String unit, String source) {
        return Map.of(
                "code", code,
                "magnitude", magnitude,
                "unit", unit,
                "source", source);
    }

    private void assertPartialReasoning(Map<String, Object> body, String code, String magnitude, String unit) {
        assertReasoningInputs(body, input(code, magnitude, unit, "PROCESS_DESCRIPTION"));
        Map<?, ?> reasoning = reasoning(body);
        assertThat(reasoning.get("calculation")).isNull();
        assertThat(reasoning.get("decision")).isNull();
    }

    private void assertEmptyReasoning(Map<String, Object> body) {
        Map<?, ?> reasoning = reasoning(body);
        assertThat((List<?>) reasoning.get("establishedInputs")).isEmpty();
        assertThat(reasoning.get("calculation")).isNull();
        assertThat(reasoning.get("decision")).isNull();
    }

    private void assertCalculation(Map<String, Object> body, String magnitude) {
        Map<?, ?> calculation = (Map<?, ?>) reasoning(body).get("calculation");
        assertThat(calculation.get("operation")).isEqualTo("MULTIPLY");
        Map<?, ?> result = (Map<?, ?>) calculation.get("result");
        assertThat(new BigDecimal(result.get("magnitude").toString())).isEqualByComparingTo(magnitude);
        assertThat(result.get("unit")).isEqualTo("MINUTE_PER_MONTH");
    }

    private void assertDecision(Map<String, Object> body, String comparison, String outcome) {
        Map<?, ?> decision = decision(body);
        assertThat(decision.get("criterion")).isEqualTo("LAB_OPERATIONAL_BURDEN_THRESHOLD");
        Map<?, ?> threshold = (Map<?, ?>) decision.get("threshold");
        assertThat(new BigDecimal(threshold.get("magnitude").toString())).isEqualByComparingTo("2400");
        assertThat(threshold.get("unit")).isEqualTo("MINUTE_PER_MONTH");
        assertThat(decision.get("comparison")).isEqualTo(comparison);
        assertThat(decision.get("outcome")).isEqualTo(outcome);
    }

    private Map<?, ?> decision(Map<String, Object> body) {
        return (Map<?, ?>) reasoning(body).get("decision");
    }

    private Map<?, ?> reasoning(Map<String, Object> body) {
        assertThat(body.get("reasoning")).isInstanceOf(Map.class);
        return (Map<?, ?>) body.get("reasoning");
    }

    private void assertNoInternalAnalysisFieldsLeak(Map<String, Object> body) {
        assertThat(body.keySet()).doesNotContain(
                "effortEvidence",
                "volumeProjection",
                "effortProjection",
                "sourceKnowledge",
                "derivedResult",
                "materialityAssessment",
                "materialityThreshold",
                "materialityEvidenceGaps",
                "evidenceGap",
                "kind",
                "decisionAffected",
                "scope",
                "establishedOperationalBurden",
                "knownFacts",
                "sourceKnowledge",
                "computableProjection",
                "businessItemRef",
                "businessItemLabel",
                "createdAt",
                "expiresAt",
                "context",
                "clarificationAnswers",
                "answer",
                "value",
                "numericValue",
                "questionId",
                "conversationId",
                "sessionId",
                "analysisId");
    }

    private void assertNoInternalAnalysisTextLeaks(String body) {
        assertThat(body).doesNotContain(
                "ProcessEffortMaterialityEvidenceGap",
                "ProcessEffortMaterialityEvidenceGapKind",
                "ProcessEvidenceGap",
                "evidenceGap",
                "kind",
                "decisionAffected",
                "SELF_REPORTED",
                "scope",
                "materialityAssessment",
                "materialityThreshold",
                "NOT_ESTABLISHED",
                "40 hours",
                "establishedOperationalBurden",
                "derivedResult",
                "sourceKnowledge",
                "knownFacts",
                "computableProjection",
                "businessItemRef",
                "businessItemLabel",
                "createdAt",
                "expiresAt",
                "context",
                "clarificationAnswers",
                "numericValue",
                "conversationId",
                "sessionId",
                "analysisId");
    }

    private void assertNoPublicReasoningLeak(String body) {
        assertThat(body).doesNotContain(
                "SOURCE_STATED",
                "DETERMINISTICALLY_DERIVED",
                "ProcessKnownFact",
                "fact-volume-per-reporting-period",
                "fact-effort-per-business-item",
                "fact-effort-per-reporting-period",
                sourceDescriptionArtifactId(),
                volumeClarificationArtifactId(),
                effortClarificationArtifactId(),
                "premiseFactIds",
                "evidenceArtifactIds",
                "businessItemRef",
                "businessItemLabel",
                "ProcessAnalysisScope",
                "ProcessQuantityProjection");
    }

    private String sourceDescriptionArtifactId() {
        return "source-" + "process-" + "description";
    }

    private String volumeClarificationArtifactId() {
        return "source-" + "clarification-" + "volume-" + "per-" + "reporting-" + "period";
    }

    private String effortClarificationArtifactId() {
        return "source-" + "clarification-" + "effort-" + "per-" + "business-" + "item";
    }

    private HttpResponse<String> post(String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/labs/process-analysis"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String lastResponseBody;

    private HttpResponse<String> postWithLocale(String locale) throws IOException, InterruptedException {
        HttpResponse<String> response = post("""
                {
                  "description": "Recibimos pedidos por WhatsApp, verificamos stock y confirmamos entrega.",
                  "locale": "%s"
                }
                """.formatted(locale));
        lastResponseBody = response.body();
        return response;
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
        return options("/api/labs/process-analysis", origin, requestHeaders);
    }

    private HttpResponse<String> options(String path, String origin, String requestHeaders)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", requestHeaders)
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> answer(String clarificationId, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + answerPath(clarificationId)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String answerPath(String clarificationId) {
        return "/api/labs/process-analysis/clarifications/" + clarificationId + "/answers";
    }

    private void assertContinuationRetryable(String clarificationId) {
        assertThat(continuationRepository.findById(new ProcessEffortClarificationContinuationId(
                        UUID.fromString(clarificationId))).orElseThrow().resolvedAt())
                .isEmpty();
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
            ProcessEffortClarificationContinuationIssuer.class,
            ProcessEffortClarificationContinuationResolutionService.class,
            ProcessEffortClarificationResolver.class,
            ProcessEffortClarificationAnswerMaterializer.class,
            ProcessEffortEstablishedKnowledgeComposer.class,
            TechnologyFitAssessmentEvaluator.class,
            ApiSecurityConfiguration.class,
            ApiCorsConfiguration.class,
            ApiExceptionHandler.class,
            ContactRequestBodyLimitFilter.class,
            ProcessEffortEvidenceProjectionMapper.class,
            ProcessEffortPerReportingPeriodMaterializer.class,
            ProcessEffortMaterialityAssessmentEvaluator.class,
            ProcessEffortMaterialityEvidenceGapIdentifier.class,
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

        @Bean
        RecordingProcessEffortClarificationContinuationRepository recordingContinuationRepository() {
            return new RecordingProcessEffortClarificationContinuationRepository();
        }

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-08-21T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    static class RecordingProcessEffortClarificationContinuationRepository
            implements ProcessEffortClarificationContinuationRepository {

        private final List<ProcessEffortClarificationContinuation> saved = new ArrayList<>();
        private RuntimeException failure;
        private boolean markResult = true;
        private int markCalls;

        @Override
        public void save(ProcessEffortClarificationContinuation continuation) {
            if (failure != null) {
                throw failure;
            }
            saved.add(continuation);
        }

        @Override
        public Optional<ProcessEffortClarificationContinuation> findById(
                ProcessEffortClarificationContinuationId id) {
            return saved.stream()
                    .filter(continuation -> continuation.id().equals(id))
                    .findFirst();
        }

        @Override
        public int deleteExpiredAtOrBefore(Instant cutoff) {
            int before = saved.size();
            saved.removeIf(continuation -> !continuation.expiresAt().isAfter(cutoff));
            return before - saved.size();
        }

        @Override
        public boolean markResolvedIfActive(
                ProcessEffortClarificationContinuationId id,
                Instant resolvedAt) {
            markCalls++;
            if (!markResult) {
                return false;
            }
            for (int index = 0; index < saved.size(); index++) {
                ProcessEffortClarificationContinuation continuation = saved.get(index);
                if (continuation.id().equals(id)
                        && !continuation.isResolved()
                        && !continuation.isExpired(resolvedAt)) {
                    saved.set(index, new ProcessEffortClarificationContinuation(
                            continuation.id(),
                            continuation.context(),
                            continuation.createdAt(),
                            continuation.expiresAt(),
                            Optional.of(resolvedAt)));
                    return true;
                }
            }
            return false;
        }

        void reset() {
            saved.clear();
            failure = null;
            markResult = true;
            markCalls = 0;
        }

        void expire(String clarificationId) {
            replace(
                    clarificationId,
                    Instant.parse("2026-08-21T11:58:00Z"),
                    Instant.parse("2026-08-21T12:00:00Z"),
                    Optional.empty());
        }

        void resolve(String clarificationId) {
            replace(
                    clarificationId,
                    null,
                    null,
                    Optional.of(Instant.parse("2026-08-21T12:00:00Z")));
        }

        private void replace(
                String clarificationId,
                Instant createdAt,
                Instant expiresAt,
                Optional<Instant> resolvedAt) {
            UUID id = UUID.fromString(clarificationId);
            for (int index = 0; index < saved.size(); index++) {
                ProcessEffortClarificationContinuation continuation = saved.get(index);
                if (continuation.id().value().equals(id)) {
                    saved.set(index, new ProcessEffortClarificationContinuation(
                            continuation.id(),
                            continuation.context(),
                            createdAt == null ? continuation.createdAt() : createdAt,
                            expiresAt == null ? continuation.expiresAt() : expiresAt,
                            resolvedAt));
                    return;
                }
            }
        }
    }

    static class FakeProcessAnalysisModelClient implements ProcessAnalysisModelClient {

        enum Mode {
            SUCCESS,
            SUCCESS_BELOW_THRESHOLD,
            VOLUME_ABSENT,
            EFFORT_ABSENT,
            BOTH_ABSENT,
            UNSUPPORTED_NON_ABSENT,
            VOLUME_ABSENT_MISMATCHED_REF,
            VOLUME_ABSENT_UNSUPPORTED_EFFORT,
            BOTH_ABSENT_BLANK_REF,
            VOLUME_ABSENT_MISSING_LABEL,
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
                case SUCCESS,
                        SUCCESS_BELOW_THRESHOLD,
                        VOLUME_ABSENT,
                        EFFORT_ABSENT,
                        BOTH_ABSENT,
                        UNSUPPORTED_NON_ABSENT,
                        VOLUME_ABSENT_MISMATCHED_REF,
                        VOLUME_ABSENT_UNSUPPORTED_EFFORT,
                        BOTH_ABSENT_BLANK_REF,
                        VOLUME_ABSENT_MISSING_LABEL -> new ProcessUnderstanding(
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
            return new ProcessAnalysisModelResult(understanding, evidenceForMode());
        }

        void reset() {
            invocations = 0;
            lastCommand = null;
            mode = Mode.SUCCESS;
        }

        private ProcessEffortEvidence evidenceForMode() {
            return switch (mode) {
                case VOLUME_ABSENT -> new ProcessEffortEvidence(
                        absentQuantity("item-1", "pedido"),
                        exactEffort("3", "item-1", "pedido"));
                case EFFORT_ABSENT -> new ProcessEffortEvidence(
                        exactVolume("4000", "item-1", "pedido"),
                        absentQuantity("item-1", "pedido"));
                case BOTH_ABSENT -> new ProcessEffortEvidence(
                        absentQuantity("item-1", "pedido"),
                        absentQuantity("item-1", "pedido"));
                case UNSUPPORTED_NON_ABSENT -> new ProcessEffortEvidence(
                        new ProcessEffortEvidenceQuantity(
                                ProcessEffortEvidenceQuantityStatus.APPROXIMATE,
                                new BigDecimal("4000"),
                                null,
                                null,
                                "item-1",
                                "pedido",
                                com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit.MONTH,
                                null,
                                "aproximadamente 4000 pedidos por mes",
                                null),
                        new ProcessEffortEvidenceQuantity(
                                ProcessEffortEvidenceQuantityStatus.RANGE,
                                null,
                                new BigDecimal("2"),
                                new BigDecimal("4"),
                                "item-1",
                                "pedido",
                                null,
                                com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit.MINUTE,
                                "entre 2 y 4 minutos por pedido",
                                null));
                case VOLUME_ABSENT_MISMATCHED_REF -> new ProcessEffortEvidence(
                        absentQuantity("item-A", "pedido"),
                        exactEffort("3", "item-B", "pedido"));
                case VOLUME_ABSENT_UNSUPPORTED_EFFORT -> new ProcessEffortEvidence(
                        absentQuantity("item-1", "pedido"),
                        new ProcessEffortEvidenceQuantity(
                                ProcessEffortEvidenceQuantityStatus.RANGE,
                                null,
                                new BigDecimal("2"),
                                new BigDecimal("4"),
                                "item-1",
                                "pedido",
                                null,
                                com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit.MINUTE,
                                "entre 2 y 4 minutos por pedido",
                                null));
                case BOTH_ABSENT_BLANK_REF -> new ProcessEffortEvidence(
                        absentQuantity(" ", "pedido"),
                        absentQuantity(" ", "pedido"));
                case VOLUME_ABSENT_MISSING_LABEL -> new ProcessEffortEvidence(
                        absentQuantity("item-1", "pedido"),
                        exactEffort("3", "item-1", " "));
                case SUCCESS, INSUFFICIENT_INFORMATION, OUT_OF_SCOPE, UNAVAILABLE, INVALID_RESPONSE -> exactEvidence();
                case SUCCESS_BELOW_THRESHOLD -> belowThresholdEvidence();
            };
        }

        private ProcessEffortEvidence exactEvidence() {
            return new ProcessEffortEvidence(
                    exactVolume("4000", "item-1", "pedido"),
                    exactEffort("2", "item-1", "pedido"));
        }

        private ProcessEffortEvidence belowThresholdEvidence() {
            return new ProcessEffortEvidence(
                    exactVolume("4", "item-1", "pedido"),
                    exactEffort("3", "item-1", "pedido"));
        }

        private ProcessEffortEvidenceQuantity exactVolume(String magnitude, String businessItemRef, String businessItemLabel) {
            return quantity(
                    ProcessEffortEvidenceQuantityStatus.EXACT,
                    magnitude,
                    businessItemRef,
                    businessItemLabel,
                    com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit.MONTH,
                    null);
        }

        private ProcessEffortEvidenceQuantity exactEffort(String magnitude, String businessItemRef, String businessItemLabel) {
            return quantity(
                    ProcessEffortEvidenceQuantityStatus.EXACT,
                    magnitude,
                    businessItemRef,
                    businessItemLabel,
                    null,
                    com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit.MINUTE);
        }

        private ProcessEffortEvidenceQuantity absentQuantity(String businessItemRef, String businessItemLabel) {
            return quantity(
                    ProcessEffortEvidenceQuantityStatus.ABSENT,
                    null,
                    businessItemRef,
                    businessItemLabel,
                    null,
                    null);
        }

        private ProcessEffortEvidenceQuantity quantity(
                ProcessEffortEvidenceQuantityStatus status,
                String magnitude,
                String businessItemRef,
                String businessItemLabel,
                com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit reportingPeriod,
                com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit effortDuration) {
            return new ProcessEffortEvidenceQuantity(
                    status,
                    magnitude == null ? null : new BigDecimal(magnitude),
                    null,
                    null,
                    businessItemRef,
                    businessItemLabel,
                    reportingPeriod,
                    effortDuration,
                    null,
                    null);
        }
    }
}
