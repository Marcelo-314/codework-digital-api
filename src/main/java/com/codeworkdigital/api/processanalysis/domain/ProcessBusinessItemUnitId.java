package com.codeworkdigital.api.processanalysis.domain;

import java.util.Objects;

/**
 * Stable semantic identity of the business item being counted or processed.
 *
 * Different business-item IDs are not interchangeable, and this type does not define hierarchy or generic work-item
 * equivalence.
 */
public record ProcessBusinessItemUnitId(String value) {

    public ProcessBusinessItemUnitId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }
}
