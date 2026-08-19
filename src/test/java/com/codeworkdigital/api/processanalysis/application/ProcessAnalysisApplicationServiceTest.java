package com.codeworkdigital.api.processanalysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessAnalysisApplicationServiceTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validCommandCallsModelExactlyOnce() {
        RecordingProcessAnalysisModelClient modelClient = new RecordingProcessAnalysisModelClient();
        RecordingTechnologyFitAssessmentEvaluator technologyFitAssessmentEvaluator =
                new RecordingTechnologyFitAssessmentEvaluator();
        ProcessAnalysisApplicationService service =
                new ProcessAnalysisApplicationService(
                        validator,
                        modelClient,
                        new ProcessEffortEvidenceProjectionMapper(),
                        technologyFitAssessmentEvaluator);
        AnalyzeProcessDescriptionCommand command = new AnalyzeProcessDescriptionCommand(
                "Receive the request, validate stock, and confirm delivery.",
                ProcessAnalysisLocale.EN);

        ProcessAnalysisResult result = service.analyze(command);
        ProcessUnderstanding understanding = result.understanding();

        assertThat(modelClient.invocations).isEqualTo(1);
        assertThat(modelClient.lastCommand).isEqualTo(command);
        assertThat(understanding.processDescription()).isEqualTo(command.description());
        assertThat(result.volumeProjection()).isPresent();
        assertThat(result.effortProjection()).isPresent();
        assertThat(result.composable()).isTrue();
        assertThat(technologyFitAssessmentEvaluator.invocations).isEqualTo(1);
        assertThat(understanding.technologyFitAssessments())
                .singleElement()
                .satisfies(assessment -> {
                    assertThat(assessment.sourceStageId()).isEqualTo("receive-request");
                    assertThat(assessment.approach()).isEqualTo(TechnologyFitApproach.TO_VALIDATE);
                });
    }

    @Test
    void invalidCommandDoesNotCallModel() {
        RecordingProcessAnalysisModelClient modelClient = new RecordingProcessAnalysisModelClient();
        RecordingTechnologyFitAssessmentEvaluator technologyFitAssessmentEvaluator =
                new RecordingTechnologyFitAssessmentEvaluator();
        ProcessAnalysisApplicationService service =
                new ProcessAnalysisApplicationService(
                        validator,
                        modelClient,
                        new ProcessEffortEvidenceProjectionMapper(),
                        technologyFitAssessmentEvaluator);
        AnalyzeProcessDescriptionCommand command = new AnalyzeProcessDescriptionCommand(
                " ",
                ProcessAnalysisLocale.ES);

        assertThatThrownBy(() -> service.analyze(command))
                .isInstanceOf(ProcessAnalysisValidationException.class);
        assertThat(modelClient.invocations).isZero();
        assertThat(technologyFitAssessmentEvaluator.invocations).isZero();
    }

    @Test
    void nonProcessStatusesDoNotExecuteTechnologyFit() {
        RecordingProcessAnalysisModelClient modelClient = new RecordingProcessAnalysisModelClient();
        modelClient.response = new ProcessAnalysisModelResult(new ProcessUnderstanding(
                "We want AI to be more efficient.",
                ProcessAnalysisStatus.INSUFFICIENT_INFORMATION,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                ""),
                exactEvidence("item-1", "item-1", "ticket", "ticket"));
        RecordingTechnologyFitAssessmentEvaluator technologyFitAssessmentEvaluator =
                new RecordingTechnologyFitAssessmentEvaluator();
        ProcessAnalysisApplicationService service =
                new ProcessAnalysisApplicationService(
                        validator,
                        modelClient,
                        new ProcessEffortEvidenceProjectionMapper(),
                        technologyFitAssessmentEvaluator);

        ProcessAnalysisResult result = service.analyze(new AnalyzeProcessDescriptionCommand(
                "We want AI to be more efficient.",
                ProcessAnalysisLocale.EN));

        ProcessUnderstanding understanding = result.understanding();
        assertThat(understanding.analysisStatus()).isEqualTo(ProcessAnalysisStatus.INSUFFICIENT_INFORMATION);
        assertThat(understanding.technologyFitAssessments()).isEmpty();
        assertThat(result.volumeProjection()).isEmpty();
        assertThat(result.effortProjection()).isEmpty();
        assertThat(result.composable()).isFalse();
        assertThat(result.effortEvidence().volumePerReportingPeriod().status())
                .isEqualTo(ProcessEffortEvidenceQuantityStatus.ABSENT);
        assertThat(result.effortEvidence().effortPerBusinessItem().status())
                .isEqualTo(ProcessEffortEvidenceQuantityStatus.ABSENT);
        assertThat(technologyFitAssessmentEvaluator.invocations).isZero();
    }

    private static final class RecordingProcessAnalysisModelClient implements ProcessAnalysisModelClient {

        private int invocations;
        private AnalyzeProcessDescriptionCommand lastCommand;
        private ProcessAnalysisModelResult response;

        @Override
        public ProcessAnalysisModelResult analyze(AnalyzeProcessDescriptionCommand command) {
            invocations++;
            lastCommand = command;
            if (response != null) {
                return response;
            }
            return new ProcessAnalysisModelResult(new ProcessUnderstanding(
                    command.description(),
                    List.of("The process starts with a request."),
                    List.of("A manual review may happen before confirmation."),
                    List.of(),
                    List.of(new ProcessUnderstandingStage(
                            "receive-request",
                            "Receive request",
                            "A request enters the process.",
                            ProcessStageProvenance.OBSERVED,
                            ProcessStageOperationType.RECEIVE,
                            ProcessStageInputNature.UNSTRUCTURED)),
                    "This understanding is preliminary."),
                    exactEvidence("item-1", "item-1", "request", "request"));
        }
    }

    private static final class RecordingTechnologyFitAssessmentEvaluator extends TechnologyFitAssessmentEvaluator {

        private int invocations;

        @Override
        public List<TechnologyFitAssessment> assess(ProcessUnderstanding understanding) {
            invocations++;
            return super.assess(understanding);
        }
    }

    private static ProcessEffortEvidence exactEvidence(
            String volumeRef,
            String effortRef,
            String volumeLabel,
            String effortLabel) {
        return new ProcessEffortEvidence(
                new ProcessEffortEvidenceQuantity(
                        ProcessEffortEvidenceQuantityStatus.EXACT,
                        new BigDecimal("4"),
                        null,
                        null,
                        volumeRef,
                        volumeLabel,
                        ProcessReportingPeriodUnit.MONTH,
                        null,
                        "4 requests per month",
                        null),
                new ProcessEffortEvidenceQuantity(
                        ProcessEffortEvidenceQuantityStatus.EXACT,
                        new BigDecimal("3"),
                        null,
                        null,
                        effortRef,
                        effortLabel,
                        null,
                        ProcessEffortDurationUnit.MINUTE,
                        "3 minutes per request",
                        null));
    }
}
