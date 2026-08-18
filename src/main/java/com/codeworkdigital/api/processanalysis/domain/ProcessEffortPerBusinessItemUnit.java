package com.codeworkdigital.api.processanalysis.domain;

import java.util.Objects;

/**
 * Unit expression representing effort-duration / business-item.
 */
public record ProcessEffortPerBusinessItemUnit(
        ProcessEffortDurationUnit effortDuration,
        ProcessBusinessItemUnitId businessItemId) implements ProcessQuantityUnitExpression {

    public ProcessEffortPerBusinessItemUnit {
        effortDuration = Objects.requireNonNull(effortDuration, "effortDuration");
        businessItemId = Objects.requireNonNull(businessItemId, "businessItemId");
    }
}
