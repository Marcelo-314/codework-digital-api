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

    @Test
    void rejectsMissingAnalysisStatus() {
        ProcessUnderstandingDraft draft = new ProcessUnderstandingDraft(
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "",
                emptyEffortEvidence());

        assertThatThrownBy(() -> ProcessUnderstandingValidator.validate(draft))
                .isInstanceOf(InvalidProcessAnalysisModelResponseException.class)
                .extracting("reason")
                .isEqualTo("analysis_status_missing");
    }

    @Test
    void acceptsOutOfScopeWithEmptyAnalyticalContent() {
        ProcessUnderstandingDraft draft = new ProcessUnderstandingDraft(
                ProcessAnalysisStatus.OUT_OF_SCOPE,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "",
                emptyEffortEvidence());

        assertThatCode(() -> ProcessUnderstandingValidator.validate(draft)).doesNotThrowAnyException();
    }

    @Test
    void acceptsInsufficientInformationWithEmptyAnalyticalContent() {
        ProcessUnderstandingDraft draft = new ProcessUnderstandingDraft(
                ProcessAnalysisStatus.INSUFFICIENT_INFORMATION,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "",
                emptyEffortEvidence());

        assertThatCode(() -> ProcessUnderstandingValidator.validate(draft)).doesNotThrowAnyException();
    }

    @Test
    void rejectsProcessIdentifiedWithoutStages() {
        ProcessUnderstandingDraft draft = new ProcessUnderstandingDraft(
                ProcessAnalysisStatus.PROCESS_IDENTIFIED,
                List.of("Observation"),
                List.of("Inference"),
                List.of("Validation question"),
                List.of(),
                "This understanding is preliminary.",
                emptyEffortEvidence());

        assertThatThrownBy(() -> ProcessUnderstandingValidator.validate(draft))
                .isInstanceOf(InvalidProcessAnalysisModelResponseException.class)
                .extracting("reason")
                .isEqualTo("stages_empty");
    }

    @Test
    void rejectsOutOfScopeWhenObservationsArePresent() {
        ProcessUnderstandingDraft draft = new ProcessUnderstandingDraft(
                ProcessAnalysisStatus.OUT_OF_SCOPE,
                List.of("This should not be present."),
                List.of(),
                List.of(),
                List.of(),
                "",
                emptyEffortEvidence());

        assertThatThrownBy(() -> ProcessUnderstandingValidator.validate(draft))
                .isInstanceOf(InvalidProcessAnalysisModelResponseException.class)
                .extracting("reason")
                .isEqualTo("observations_must_be_empty");
    }

    private ProcessUnderstandingDraft draftWithObservations(int observationCount) {
        List<String> observations = new ArrayList<>();
        for (int index = 0; index < observationCount; index++) {
            observations.add("Observation " + index);
        }

        return new ProcessUnderstandingDraft(
                ProcessAnalysisStatus.PROCESS_IDENTIFIED,
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
                "This understanding is preliminary.",
                emptyEffortEvidence());
    }

    private ProcessEffortEvidence emptyEffortEvidence() {
        return new ProcessEffortEvidence(
                absentQuantity(),
                absentQuantity());
    }

    private ProcessEffortEvidenceQuantity absentQuantity() {
        return new ProcessEffortEvidenceQuantity(
                ProcessEffortEvidenceQuantityStatus.ABSENT,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
