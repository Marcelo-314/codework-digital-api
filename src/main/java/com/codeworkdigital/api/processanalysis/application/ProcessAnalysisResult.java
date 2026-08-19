package com.codeworkdigital.api.processanalysis.application;

import com.codeworkdigital.api.processanalysis.domain.ProcessQuantityProjection;
import java.util.Objects;
import java.util.Optional;

public record ProcessAnalysisResult(
        ProcessUnderstanding understanding,
        ProcessEffortEvidence effortEvidence,
        Optional<ProcessQuantityProjection> volumeProjection,
        Optional<ProcessQuantityProjection> effortProjection,
        ProcessEffortSourceKnowledge sourceKnowledge,
        boolean composable) {

    public ProcessAnalysisResult {
        understanding = Objects.requireNonNull(understanding, "understanding");
        effortEvidence = Objects.requireNonNull(effortEvidence, "effortEvidence");
        volumeProjection = Objects.requireNonNull(volumeProjection, "volumeProjection");
        effortProjection = Objects.requireNonNull(effortProjection, "effortProjection");
        sourceKnowledge = Objects.requireNonNull(sourceKnowledge, "sourceKnowledge");
    }
}
