package com.codeworkdigital.api.processanalysis.application;

import java.util.Objects;

public record ProcessEffortEvidence(
        ProcessEffortEvidenceQuantity volumePerReportingPeriod,
        ProcessEffortEvidenceQuantity effortPerBusinessItem) {

    public ProcessEffortEvidence {
        volumePerReportingPeriod = Objects.requireNonNull(volumePerReportingPeriod, "volumePerReportingPeriod");
        effortPerBusinessItem = Objects.requireNonNull(effortPerBusinessItem, "effortPerBusinessItem");
    }

    public static ProcessEffortEvidence empty() {
        return new ProcessEffortEvidence(absentQuantity(), absentQuantity());
    }

    private static ProcessEffortEvidenceQuantity absentQuantity() {
        return new ProcessEffortEvidenceQuantity(
                ProcessEffortEvidenceQuantityStatus.ABSENT,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
