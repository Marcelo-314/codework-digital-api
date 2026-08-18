package com.codeworkdigital.api.processanalysis.domain;

import java.util.Objects;

/**
 * Concrete, identifiable artifact that may serve as evidential support for process-analysis knowledge.
 *
 * This type intentionally models only artifact identity and epistemic kind. It does not model fact support,
 * physical provenance, source spans, citations, or deterministic derivations.
 */
public record ProcessEvidenceArtifact(
        ProcessEvidenceArtifactId id,
        ProcessEvidenceArtifactKind kind,
        String description) {

    public ProcessEvidenceArtifact {
        id = Objects.requireNonNull(id, "id");
        kind = Objects.requireNonNull(kind, "kind");
        description = requireNonBlank(description, "description");
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
