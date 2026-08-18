package com.codeworkdigital.api.processanalysis.domain;

import java.util.Objects;

/**
 * Stable semantic identity of a category domain within a process-analysis model.
 *
 * The value is an opaque domain reference token. It is not a display label, database id, Java enum class name,
 * persistence contract, or proof that the domain is exhaustive.
 */
public record ProcessCategoryDomainId(String value) {

    public ProcessCategoryDomainId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }
}
