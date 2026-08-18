package com.codeworkdigital.api.processanalysis.domain;

import java.util.Objects;

/**
 * Unit expression representing business-item / reporting-period.
 */
public record ProcessBusinessItemPerReportingPeriodUnit(
        ProcessBusinessItemUnitId businessItemId,
        ProcessReportingPeriodUnit reportingPeriod) implements ProcessQuantityUnitExpression {

    public ProcessBusinessItemPerReportingPeriodUnit {
        businessItemId = Objects.requireNonNull(businessItemId, "businessItemId");
        reportingPeriod = Objects.requireNonNull(reportingPeriod, "reportingPeriod");
    }
}
