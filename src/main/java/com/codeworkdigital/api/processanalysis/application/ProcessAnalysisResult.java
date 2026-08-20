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
        Optional<ProcessEffortDerivedResult> derivedResult,
        ProcessEffortMaterialityAssessment materialityAssessment,
        boolean composable) {

    public ProcessAnalysisResult {
        understanding = Objects.requireNonNull(understanding, "understanding");
        effortEvidence = Objects.requireNonNull(effortEvidence, "effortEvidence");
        volumeProjection = Objects.requireNonNull(volumeProjection, "volumeProjection");
        effortProjection = Objects.requireNonNull(effortProjection, "effortProjection");
        sourceKnowledge = Objects.requireNonNull(sourceKnowledge, "sourceKnowledge");
        derivedResult = Objects.requireNonNull(derivedResult, "derivedResult");
        materialityAssessment = Objects.requireNonNull(materialityAssessment, "materialityAssessment");
    }

    public ProcessAnalysisResult withDerivedResult(Optional<ProcessEffortDerivedResult> derivedResult) {
        return new ProcessAnalysisResult(
                understanding,
                effortEvidence,
                volumeProjection,
                effortProjection,
                sourceKnowledge,
                derivedResult,
                materialityAssessment,
                composable);
    }

    public ProcessAnalysisResult withMaterialityAssessment(
            ProcessEffortMaterialityAssessment materialityAssessment) {
        return new ProcessAnalysisResult(
                understanding,
                effortEvidence,
                volumeProjection,
                effortProjection,
                sourceKnowledge,
                derivedResult,
                materialityAssessment,
                composable);
    }
}
