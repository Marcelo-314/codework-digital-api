package com.codeworkdigital.api.processanalysis.domain;

import java.util.List;

public record ProcessAnalysisKnowledge(
        List<ProcessKnownFact> knownFacts,
        List<ProcessInference> inferences,
        List<ProcessEvidenceGap> evidenceGaps,
        List<ProcessConstraint> constraints) {

    public ProcessAnalysisKnowledge {
        knownFacts = List.copyOf(knownFacts);
        inferences = List.copyOf(inferences);
        evidenceGaps = List.copyOf(evidenceGaps);
        constraints = List.copyOf(constraints);
    }
}
