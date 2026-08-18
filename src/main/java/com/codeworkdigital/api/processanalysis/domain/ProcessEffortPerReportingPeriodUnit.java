package com.codeworkdigital.api.processanalysis.domain;

import java.util.Objects;

/**
 * Unit expression representing effort-duration / reporting-period.
 */
public record ProcessEffortPerReportingPeriodUnit(
        ProcessEffortDurationUnit effortDuration,
        ProcessReportingPeriodUnit reportingPeriod) implements ProcessQuantityUnitExpression {

    public ProcessEffortPerReportingPeriodUnit {
        effortDuration = Objects.requireNonNull(effortDuration, "effortDuration");
        reportingPeriod = Objects.requireNonNull(reportingPeriod, "reportingPeriod");
    }
}
