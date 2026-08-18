package com.codeworkdigital.api.processanalysis.domain;

import java.util.Objects;

/**
 * Concrete, identifiable artifact that may serve as evidential support for process-analysis knowledge.
 *
 * This type intentionally models only stable domain identity, epistemic kind, and a human-readable description.
 * The description is descriptive only: it is not physical provenance, a locator, a citation, a source span, or a
 * fact-support relationship, and must not be used as a textual substitute for verifiable provenance.
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
