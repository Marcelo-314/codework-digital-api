package com.codeworkdigital.api.processanalysis.domain;

import java.util.HashSet;
import java.util.Objects;

/**
 * P06 relation declaration: business-item volume per reporting period multiplied by effort per business item yields
 * effort duration per reporting period.
 */
public record ProcessEffortPerReportingPeriodDerivation(
        ProcessKnownFactId volumeFactId,
        ProcessKnownFactId effortPerBusinessItemFactId,
        ProcessKnownFactId resultFactId) implements ProcessDeterministicDerivation {

    public ProcessEffortPerReportingPeriodDerivation {
        volumeFactId = Objects.requireNonNull(volumeFactId, "volumeFactId");
        effortPerBusinessItemFactId =
                Objects.requireNonNull(effortPerBusinessItemFactId, "effortPerBusinessItemFactId");
        resultFactId = Objects.requireNonNull(resultFactId, "resultFactId");

        HashSet<ProcessKnownFactId> factIds = new HashSet<>();
        factIds.add(volumeFactId);
        factIds.add(effortPerBusinessItemFactId);
        factIds.add(resultFactId);
        if (factIds.size() != 3) {
            throw new IllegalArgumentException("deterministic derivation fact ids must be distinct");
        }
    }
}
