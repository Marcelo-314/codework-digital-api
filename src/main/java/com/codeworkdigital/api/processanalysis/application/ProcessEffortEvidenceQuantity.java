package com.codeworkdigital.api.processanalysis.application;

import com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit;
import java.math.BigDecimal;

public record ProcessEffortEvidenceQuantity(
        ProcessEffortEvidenceQuantityStatus status,
        BigDecimal magnitude,
        BigDecimal minMagnitude,
        BigDecimal maxMagnitude,
        String businessItemRef,
        String businessItemLabel,
        ProcessReportingPeriodUnit reportingPeriod,
        ProcessEffortDurationUnit effortDuration,
        String evidenceText,
        String note) {
}
