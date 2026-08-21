package com.codeworkdigital.api.processanalysis.application;

import java.math.BigDecimal;
import java.util.Objects;

public record ProcessEffortMaterialityClarificationAnswer(
        ProcessEffortMaterialityEvidenceGapKind kind,
        BigDecimal magnitude) {

    public ProcessEffortMaterialityClarificationAnswer {
        kind = Objects.requireNonNull(kind, "kind");
        magnitude = Objects.requireNonNull(magnitude, "magnitude");
        if (magnitude.signum() < 0) {
            throw new IllegalArgumentException("magnitude must be greater than or equal to zero");
        }
    }
}
