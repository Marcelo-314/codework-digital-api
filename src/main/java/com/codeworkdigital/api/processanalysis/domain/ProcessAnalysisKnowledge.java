package com.codeworkdigital.api.processanalysis.domain;

import java.util.List;

public record ProcessAnalysisKnowledge(
        List<ProcessKnownFact> knownFacts,
        List<ProcessEvidenceGap> evidenceGaps,
        List<ProcessConstraint> constraints) {

    public ProcessAnalysisKnowledge {
        knownFacts = List.copyOf(knownFacts);
        evidenceGaps = List.copyOf(evidenceGaps);
        constraints = List.copyOf(constraints);
    }
}
