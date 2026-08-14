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
        ProcessAnalysisApplicationService service = new ProcessAnalysisApplicationService(validator, modelClient);
        AnalyzeProcessDescriptionCommand command = new AnalyzeProcessDescriptionCommand(
                "Receive the request, validate stock, and confirm delivery.",
                ProcessAnalysisLocale.EN);

        ProcessUnderstanding understanding = service.analyze(command);

        assertThat(modelClient.invocations).isEqualTo(1);
        assertThat(modelClient.lastCommand).isEqualTo(command);
        assertThat(understanding.processDescription()).isEqualTo(command.description());
    }

    @Test
    void invalidCommandDoesNotCallModel() {
        RecordingProcessAnalysisModelClient modelClient = new RecordingProcessAnalysisModelClient();
        ProcessAnalysisApplicationService service = new ProcessAnalysisApplicationService(validator, modelClient);
        AnalyzeProcessDescriptionCommand command = new AnalyzeProcessDescriptionCommand(
                " ",
                ProcessAnalysisLocale.ES);

        assertThatThrownBy(() -> service.analyze(command))
                .isInstanceOf(ProcessAnalysisValidationException.class);
        assertThat(modelClient.invocations).isZero();
    }

    private static final class RecordingProcessAnalysisModelClient implements ProcessAnalysisModelClient {

        private int invocations;
        private AnalyzeProcessDescriptionCommand lastCommand;

        @Override
        public ProcessUnderstanding analyze(AnalyzeProcessDescriptionCommand command) {
            invocations++;
            lastCommand = command;
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
}
