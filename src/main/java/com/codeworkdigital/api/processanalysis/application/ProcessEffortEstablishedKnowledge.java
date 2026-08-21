package com.codeworkdigital.api.processanalysis.application;

import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceArtifactId;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceArtifactKind;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceBase;
import com.codeworkdigital.api.processanalysis.domain.ProcessFactGrounding;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFact;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFactId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ProcessEffortEstablishedKnowledge(
        ProcessEvidenceBase evidenceBase,
        List<ProcessKnownFact> knownFacts) {

    private static final Set<ProcessKnownFactId> SUPPORTED_FACT_IDS = Set.of(
            ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
            ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID);

    public ProcessEffortEstablishedKnowledge {
        evidenceBase = Objects.requireNonNull(evidenceBase, "evidenceBase");
        knownFacts = List.copyOf(knownFacts);

        Set<ProcessEvidenceArtifactId> artifactIds = new HashSet<>();
        evidenceBase.artifacts().forEach(artifact -> {
            if (artifact.kind() != ProcessEvidenceArtifactKind.SOURCE_MATERIAL) {
                throw new IllegalArgumentException("established effort evidence artifacts must be source material");
            }
            artifactIds.add(artifact.id());
        });

        Set<ProcessKnownFactId> factIds = new HashSet<>();
        for (ProcessKnownFact knownFact : knownFacts) {
            if (!SUPPORTED_FACT_IDS.contains(knownFact.id())) {
                throw new IllegalArgumentException("unsupported established effort fact id: " + knownFact.id().value());
            }
            if (!factIds.add(knownFact.id())) {
                throw new IllegalArgumentException("established effort fact id must be unique: " + knownFact.id().value());
            }
            if (knownFact.grounding() != ProcessFactGrounding.SOURCE_STATED) {
                throw new IllegalArgumentException("established effort facts must be source stated");
            }
            if (!knownFact.scope().isProcessWide()) {
                throw new IllegalArgumentException("established effort facts must be process-wide");
            }
            if (!knownFact.premiseFactIds().isEmpty()) {
                throw new IllegalArgumentException("established effort facts must not have premise fact ids");
            }
            if (knownFact.evidenceArtifactIds().size() != 1) {
                throw new IllegalArgumentException("established effort fact must reference exactly one evidence artifact");
            }
            if (!artifactIds.contains(knownFact.evidenceArtifactIds().getFirst())) {
                throw new IllegalArgumentException("established effort fact references an unknown evidence artifact");
            }
        }
    }
}
