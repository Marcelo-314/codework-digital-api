package com.codeworkdigital.api.processanalysis.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessUnderstandingValidatorTest {

    @Test
    void acceptsObservationsAtMaximumSize() {
        ProcessUnderstandingDraft draft = draftWithObservations(ProcessUnderstandingConstraints.MAX_OBSERVATIONS);

        assertThatCode(() -> ProcessUnderstandingValidator.validate(draft)).doesNotThrowAnyException();
    }

    @Test
    void rejectsObservationsAboveMaximumSize() {
        ProcessUnderstandingDraft draft = draftWithObservations(ProcessUnderstandingConstraints.MAX_OBSERVATIONS + 1);

        assertThatThrownBy(() -> ProcessUnderstandingValidator.validate(draft))
                .isInstanceOf(InvalidProcessAnalysisModelResponseException.class)
                .extracting("reason")
                .isEqualTo("observations_too_large");
    }

    private ProcessUnderstandingDraft draftWithObservations(int observationCount) {
        List<String> observations = new ArrayList<>();
        for (int index = 0; index < observationCount; index++) {
            observations.add("Observation " + index);
        }

        return new ProcessUnderstandingDraft(
                observations,
                List.of("Inference"),
                List.of("Validation question"),
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
