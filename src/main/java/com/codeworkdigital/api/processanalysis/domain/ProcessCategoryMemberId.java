package com.codeworkdigital.api.processanalysis.domain;

import java.util.Objects;

/**
 * Stable semantic identity of one member inside a category domain.
 *
 * The value is an opaque member reference token. It is not a display label, arbitrary free-form fact content, or proof
 * that the member belongs to some separately declared exhaustive set.
 */
public record ProcessCategoryMemberId(String value) {

    public ProcessCategoryMemberId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }
}
