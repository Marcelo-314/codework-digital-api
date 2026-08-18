package com.codeworkdigital.api.processanalysis.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Bounded process-analysis quantity projection for the P06 intervention-value case.
 *
 * The unit is limited to the currently demonstrated P06 forms. This type does not implement multiplication,
 * cancellation, conversion, deterministic derivation, or generic dimensional analysis.
 */
public record ProcessQuantityProjection(
        BigDecimal magnitude,
        ProcessQuantityUnitExpression unit) implements ProcessComputableProjection {

    public ProcessQuantityProjection {
        magnitude = normalize(Objects.requireNonNull(magnitude, "magnitude"));
        unit = Objects.requireNonNull(unit, "unit");
    }

    private static BigDecimal normalize(BigDecimal magnitude) {
        if (magnitude.signum() < 0) {
            throw new IllegalArgumentException("magnitude must be greater than or equal to zero");
        }
        if (magnitude.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return magnitude.stripTrailingZeros();
    }
}
