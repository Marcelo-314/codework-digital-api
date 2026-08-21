package com.codeworkdigital.api.processanalysis.application;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

record ProcessEffortClarificationContext(
        ProcessEffortEvidence effortEvidence,
        ProcessEffortSourceKnowledge sourceKnowledge,
        ProcessEffortMaterialityAssessment materialityAssessment,
        List<ProcessEffortMaterialityEvidenceGap> actionableGaps) {

    ProcessEffortClarificationContext {
        effortEvidence = Objects.requireNonNull(effortEvidence, "effortEvidence");
        sourceKnowledge = Objects.requireNonNull(sourceKnowledge, "sourceKnowledge");
        materialityAssessment = Objects.requireNonNull(materialityAssessment, "materialityAssessment");
        actionableGaps = List.copyOf(actionableGaps);

        if (materialityAssessment.status() != ProcessEffortMaterialityAssessmentStatus.NOT_ESTABLISHED) {
            throw new IllegalArgumentException("clarification materiality assessment must be not established");
        }
        if (!materialityAssessment.threshold().equals(ProcessEffortMaterialityThreshold.P06_LAB_POLICY)) {
            throw new IllegalArgumentException("clarification materiality assessment must use P06 lab policy");
        }
        if (actionableGaps.isEmpty()) {
            throw new IllegalArgumentException("clarification context must have actionable materiality evidence gaps");
        }
        requireNoDuplicateGapKinds(actionableGaps);
    }

    static ProcessEffortClarificationContext from(ProcessAnalysisResult result) {
        Objects.requireNonNull(result, "result");
        if (!result.understanding().isProcessIdentified()) {
            throw new IllegalArgumentException("baseline must identify a process");
        }
        ProcessEffortMaterialityAssessment assessment = result.materialityAssessment()
                .orElseThrow(() -> new IllegalArgumentException("baseline materiality assessment is required"));
        return new ProcessEffortClarificationContext(
                result.effortEvidence(),
                result.sourceKnowledge(),
                assessment,
                result.materialityEvidenceGaps());
    }

    private static void requireNoDuplicateGapKinds(List<ProcessEffortMaterialityEvidenceGap> gaps) {
        Set<ProcessEffortMaterialityEvidenceGapKind> kinds =
                EnumSet.noneOf(ProcessEffortMaterialityEvidenceGapKind.class);
        for (ProcessEffortMaterialityEvidenceGap gap : gaps) {
            ProcessEffortMaterialityEvidenceGapKind kind = Objects.requireNonNull(gap, "gap").kind();
            if (!kinds.add(kind)) {
                throw new IllegalArgumentException("duplicate actionable gap kind: " + kind);
            }
        }
    }
}
