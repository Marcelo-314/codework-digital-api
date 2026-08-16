package com.codeworkdigital.api.processanalysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
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
                new ProcessAnalysisApplicationService(validator, modelClient, technologyFitAssessmentEvaluator);
        AnalyzeProcessDescriptionCommand command = new AnalyzeProcessDescriptionCommand(
                "Receive the request, validate stock, and confirm delivery.",
                ProcessAnalysisLocale.EN);

        ProcessUnderstanding understanding = service.analyze(command);

        assertThat(modelClient.invocations).isEqualTo(1);
        assertThat(modelClient.lastCommand).isEqualTo(command);
        assertThat(understanding.processDescription()).isEqualTo(command.description());
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
                new ProcessAnalysisApplicationService(validator, modelClient, technologyFitAssessmentEvaluator);
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
        modelClient.response = new ProcessUnderstanding(
                "We want AI to be more efficient.",
                ProcessAnalysisStatus.INSUFFICIENT_INFORMATION,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "");
        RecordingTechnologyFitAssessmentEvaluator technologyFitAssessmentEvaluator =
                new RecordingTechnologyFitAssessmentEvaluator();
        ProcessAnalysisApplicationService service =
                new ProcessAnalysisApplicationService(validator, modelClient, technologyFitAssessmentEvaluator);

        ProcessUnderstanding understanding = service.analyze(new AnalyzeProcessDescriptionCommand(
                "We want AI to be more efficient.",
                ProcessAnalysisLocale.EN));

        assertThat(understanding.analysisStatus()).isEqualTo(ProcessAnalysisStatus.INSUFFICIENT_INFORMATION);
        assertThat(understanding.technologyFitAssessments()).isEmpty();
        assertThat(technologyFitAssessmentEvaluator.invocations).isZero();
    }

    private static final class RecordingProcessAnalysisModelClient implements ProcessAnalysisModelClient {

        private int invocations;
        private AnalyzeProcessDescriptionCommand lastCommand;
        private ProcessUnderstanding response;

        @Override
        public ProcessUnderstanding analyze(AnalyzeProcessDescriptionCommand command) {
            invocations++;
            lastCommand = command;
            if (response != null) {
                return response;
            }
            return new ProcessUnderstanding(
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
                    "This understanding is preliminary.");
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
}
