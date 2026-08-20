package com.codeworkdigital.api.processanalysis.application;

import com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessQuantityProjection;
import com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit;
import java.math.BigDecimal;
import java.util.Objects;

public record ProcessEffortMaterialityThreshold(ProcessQuantityProjection projection) {

    public static final ProcessEffortMaterialityThreshold P06_LAB_POLICY =
            new ProcessEffortMaterialityThreshold(new ProcessQuantityProjection(
                    new BigDecimal("2400"),
                    new ProcessEffortPerReportingPeriodUnit(
                            ProcessEffortDurationUnit.MINUTE,
                            ProcessReportingPeriodUnit.MONTH)));

    public ProcessEffortMaterialityThreshold {
        projection = Objects.requireNonNull(projection, "projection");
        if (!(projection.unit() instanceof ProcessEffortPerReportingPeriodUnit unit)
                || unit.effortDuration() != ProcessEffortDurationUnit.MINUTE
                || unit.reportingPeriod() != ProcessReportingPeriodUnit.MONTH) {
            throw new IllegalArgumentException(
                    "process effort materiality threshold must be minute per month");
        }
    }
}
