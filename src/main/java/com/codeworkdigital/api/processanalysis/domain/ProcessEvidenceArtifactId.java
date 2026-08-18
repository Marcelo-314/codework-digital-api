package com.codeworkdigital.api.processanalysis.domain;

import java.util.Objects;

/**
 * Stable identity for an evidence artifact inside a process-analysis model.
 *
 * The value is a domain reference token, not a URL, database id, source offset, path, or persistence contract.
 */
public record ProcessEvidenceArtifactId(String value) {

    public ProcessEvidenceArtifactId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }
}
