package com.codeworkdigital.api.processanalysis.application;

import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceBase;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFact;
import java.util.List;
import java.util.Objects;

public record ProcessEffortSourceKnowledge(
        ProcessEvidenceBase evidenceBase,
        List<ProcessKnownFact> knownFacts) {

    public ProcessEffortSourceKnowledge {
        evidenceBase = Objects.requireNonNull(evidenceBase, "evidenceBase");
        knownFacts = List.copyOf(knownFacts);
        if (!knownFacts.isEmpty()) {
            if (evidenceBase.artifacts().size() != 1) {
                throw new IllegalArgumentException("source facts require exactly one evidence artifact");
            }
            for (ProcessKnownFact knownFact : knownFacts) {
                if (!knownFact.evidenceArtifactIds().equals(List.of(evidenceBase.artifacts().getFirst().id()))) {
                    throw new IllegalArgumentException("source fact must reference the source evidence artifact");
                }
            }
        }
    }
}
