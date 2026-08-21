package com.codeworkdigital.api.processanalysis.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisApplicationService;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisModelClient;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisModelResult;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisResult;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisStatus;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationAnswerMaterializer;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContinuation;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContinuationId;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContinuationIssuer;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContinuationRepository;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContinuationResolutionService;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContext;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationResolver;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortEstablishedKnowledgeComposer;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortEvidence;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortEvidenceProjectionMapper;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortEvidenceQuantity;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortEvidenceQuantityStatus;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortMaterialityAssessment;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortMaterialityAssessmentEvaluator;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortMaterialityEvidenceGap;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortMaterialityEvidenceGapIdentifier;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortMaterialityEvidenceGapKind;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortMaterialityThreshold;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortPerReportingPeriodMaterializer;
import com.codeworkdigital.api.processanalysis.application.ProcessStageInputNature;
import com.codeworkdigital.api.processanalysis.application.ProcessStageOperationType;
import com.codeworkdigital.api.processanalysis.application.ProcessStageProvenance;
import com.codeworkdigital.api.processanalysis.application.ProcessUnderstanding;
import com.codeworkdigital.api.processanalysis.application.ProcessUnderstandingStage;
import com.codeworkdigital.api.processanalysis.application.TechnologyFitAssessmentEvaluator;
import com.codeworkdigital.api.processanalysis.domain.ProcessAnalysisScope;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceGap;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceSource;
import com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.infrastructure.persistence.JdbcProcessEffortClarificationContinuationRepository;
import com.codeworkdigital.api.shared.error.ApiExceptionHandler;
import com.codeworkdigital.api.shared.security.ApiSecurityConfiguration;
import com.codeworkdigital.api.shared.web.ApiCorsConfiguration;
import com.codeworkdigital.api.shared.web.ContactRequestBodyLimitFilter;
import com.codeworkdigital.api.support.PostgreSqlIntegrationTestSupport;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
        classes = ProcessClarificationAnswerPersistenceApiIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.health.db.enabled=false")
class ProcessClarificationAnswerPersistenceApiIntegrationTest extends PostgreSqlIntegrationTestSupport {

    private static final Instant NOW = Instant.parse("2026-08-21T12:00:00Z");

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ProcessEffortEvidenceProjectionMapper sourceMapper = new ProcessEffortEvidenceProjectionMapper();

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProcessEffortClarificationContinuationRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void cleanDatabase() {
        jdbcClient.sql("DELETE FROM process_effort_clarification_continuation").update();
    }

    @Test
    void publicClarificationAnswerLoadsRealContinuationAndAtomicallyMarksResolvedAt() throws Exception {
        ProcessEffortClarificationContinuation saved = ProcessEffortClarificationContinuation.create(
                contextWithVolumeGap(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        repository.save(saved);

        HttpResponse<String> response = answer(saved.id(), """
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
        assertThat(body.get("clarificationId")).isEqualTo(saved.id().value().toString());
        assertThat(repository.findById(saved.id()).orElseThrow().resolvedAt()).contains(NOW);

        HttpResponse<String> second = answer(saved.id(), """
                {
                  "answers": [
                    {
                      "code": "VOLUME_PER_REPORTING_PERIOD",
                      "value": 4000
                    }
                  ]
                }
                """);
        assertThat(second.statusCode()).isEqualTo(409);
        assertThat(repository.findById(saved.id()).orElseThrow().resolvedAt()).contains(NOW);
    }

    private ProcessEffortClarificationContext contextWithVolumeGap() {
        ProcessEffortEvidence evidence = new ProcessEffortEvidence(
                quantity(ProcessEffortEvidenceQuantityStatus.ABSENT, null, "ticket-ref", "ticket", null, null),
                quantity(
                        ProcessEffortEvidenceQuantityStatus.EXACT,
                        "2",
                        "ticket-ref",
                        "ticket",
                        null,
                        ProcessEffortDurationUnit.MINUTE));
        ProcessAnalysisResult mapped = sourceMapper.map(new ProcessAnalysisModelResult(understanding(), evidence));
        return ProcessEffortClarificationContext.from(new ProcessAnalysisResult(
                mapped.understanding(),
                mapped.effortEvidence(),
                mapped.volumeProjection(),
                mapped.effortProjection(),
                mapped.sourceKnowledge(),
                Optional.empty(),
                Optional.of(ProcessEffortMaterialityAssessment.notEstablished(
                        ProcessEffortMaterialityThreshold.P06_LAB_POLICY)),
                List.of(new ProcessEffortMaterialityEvidenceGap(
                        ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD,
                        new ProcessEvidenceGap(
                                "What monthly quantity do you use as the reference volume for this process?",
                                ProcessEvidenceSource.SELF_REPORTED,
                                "P06 materiality assessment cannot be established without this value",
                                ProcessAnalysisScope.processWide()))),
                mapped.composable()));
    }

    private ProcessEffortEvidenceQuantity quantity(
            ProcessEffortEvidenceQuantityStatus status,
            String magnitude,
            String businessItemRef,
            String businessItemLabel,
            ProcessReportingPeriodUnit reportingPeriod,
            ProcessEffortDurationUnit effortDuration) {
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

    private ProcessUnderstanding understanding() {
        return new ProcessUnderstanding(
                "Description",
                List.of("A request is received."),
                List.of("A manual check may occur."),
                List.of(),
                List.of(new ProcessUnderstandingStage(
                        "receive-request",
                        "Receive request",
                        "The team receives a request.",
                        ProcessStageProvenance.OBSERVED,
                        ProcessStageOperationType.RECEIVE,
                        ProcessStageInputNature.UNSTRUCTURED)),
                "This understanding is preliminary.");
    }

    private HttpResponse<String> answer(ProcessEffortClarificationContinuationId id, String body)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port
                        + "/api/labs/process-analysis/clarifications/" + id.value() + "/answers"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> json(HttpResponse<String> response) throws Exception {
        return objectMapper.readValue(response.body(), Map.class);
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
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
            JdbcProcessEffortClarificationContinuationRepository.class,
            TestConfig.class
    })
    static class TestApplication {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {

        @Bean
        ProcessAnalysisModelClient fakeProcessAnalysisModelClient() {
            return command -> new ProcessAnalysisModelResult(new ProcessUnderstanding(
                    command.description(),
                    ProcessAnalysisStatus.OUT_OF_SCOPE,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    ""), new ProcessEffortEvidence(
                    new ProcessEffortEvidenceQuantity(
                            ProcessEffortEvidenceQuantityStatus.ABSENT,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null),
                    new ProcessEffortEvidenceQuantity(
                            ProcessEffortEvidenceQuantityStatus.ABSENT,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null)));
        }

        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
