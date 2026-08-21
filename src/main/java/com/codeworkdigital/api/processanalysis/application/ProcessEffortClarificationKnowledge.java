package com.codeworkdigital.api.processanalysis.application;

import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceArtifactId;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceArtifactKind;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceBase;
import com.codeworkdigital.api.processanalysis.domain.ProcessFactGrounding;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFact;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ProcessEffortClarificationKnowledge(
        ProcessEvidenceBase evidenceBase,
        List<ProcessKnownFact> knownFacts) {

    public ProcessEffortClarificationKnowledge {
        evidenceBase = Objects.requireNonNull(evidenceBase, "evidenceBase");
        knownFacts = List.copyOf(knownFacts);

        Set<ProcessEvidenceArtifactId> artifactIds = new HashSet<>();
        evidenceBase.artifacts().forEach(artifact -> {
            if (artifact.kind() != ProcessEvidenceArtifactKind.SOURCE_MATERIAL) {
                throw new IllegalArgumentException("clarification evidence artifacts must be source material");
            }
            artifactIds.add(artifact.id());
        });

        for (ProcessKnownFact knownFact : knownFacts) {
            if (knownFact.grounding() != ProcessFactGrounding.SOURCE_STATED) {
                throw new IllegalArgumentException("clarification facts must be source stated");
            }
            if (knownFact.evidenceArtifactIds().size() != 1) {
                throw new IllegalArgumentException("clarification fact must reference exactly one evidence artifact");
            }
            if (!artifactIds.contains(knownFact.evidenceArtifactIds().getFirst())) {
                throw new IllegalArgumentException("clarification fact must reference a clarification evidence artifact");
            }
        }
    }
}
