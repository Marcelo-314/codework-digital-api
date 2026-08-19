package com.codeworkdigital.api.processanalysis.application;

import java.util.Objects;

public record ProcessAnalysisModelResult(
        ProcessUnderstanding understanding,
        ProcessEffortEvidence effortEvidence) {

    public ProcessAnalysisModelResult {
        understanding = Objects.requireNonNull(understanding, "understanding");
        effortEvidence = Objects.requireNonNull(effortEvidence, "effortEvidence");
    }
}
