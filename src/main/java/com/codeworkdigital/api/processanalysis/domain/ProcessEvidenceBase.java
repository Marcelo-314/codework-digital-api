package com.codeworkdigital.api.processanalysis.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Canonical set of evidence artifacts available inside one process analysis.
 *
 * This is not a persistence registry, physical provenance registry, or set of fact-support relationships. An artifact
 * belonging to the evidence base does not mean it supports every fact in the analysis.
 */
public record ProcessEvidenceBase(List<ProcessEvidenceArtifact> artifacts) {

    public ProcessEvidenceBase {
        artifacts = List.copyOf(artifacts);

        Set<ProcessEvidenceArtifactId> artifactIds = new HashSet<>();
        for (ProcessEvidenceArtifact artifact : artifacts) {
            ProcessEvidenceArtifactId artifactId = artifact.id();
            if (!artifactIds.add(artifactId)) {
                throw new IllegalArgumentException("evidence artifact id must be unique: " + artifactId.value());
            }
        }
    }
}
