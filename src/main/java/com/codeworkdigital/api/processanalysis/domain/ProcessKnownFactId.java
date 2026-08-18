package com.codeworkdigital.api.processanalysis.domain;

import java.util.Objects;

/**
 * Stable identity for a known fact inside a process-analysis model.
 *
 * The value is a domain reference token, not a database id, list index, statement hash, persistence contract, or
 * runtime/OpenAI identity.
 */
public record ProcessKnownFactId(String value) {

    public ProcessKnownFactId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }
}
