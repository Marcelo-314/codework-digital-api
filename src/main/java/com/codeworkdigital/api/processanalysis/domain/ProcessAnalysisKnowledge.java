package com.codeworkdigital.api.processanalysis.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        Set<ProcessKnownFactId> knownFactIds = new HashSet<>();
        for (ProcessKnownFact knownFact : knownFacts) {
            ProcessKnownFactId knownFactId = knownFact.id();
            if (!knownFactIds.add(knownFactId)) {
                throw new IllegalArgumentException("known fact id must be unique: " + knownFactId.value());
            }
        }
    }
}
