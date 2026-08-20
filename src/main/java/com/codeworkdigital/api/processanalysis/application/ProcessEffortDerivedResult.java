package com.codeworkdigital.api.processanalysis.application;

import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerReportingPeriodDerivation;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFact;
import java.util.Objects;

public record ProcessEffortDerivedResult(
        ProcessKnownFact resultFact,
        ProcessEffortPerReportingPeriodDerivation derivation) {

    public ProcessEffortDerivedResult {
        resultFact = Objects.requireNonNull(resultFact, "resultFact");
        derivation = Objects.requireNonNull(derivation, "derivation");
    }
}
