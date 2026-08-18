package com.codeworkdigital.api.processanalysis.application;

import com.codeworkdigital.api.processanalysis.domain.ProcessAnalysisKnowledge;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerReportingPeriodDerivation;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFact;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFactId;
import com.codeworkdigital.api.processanalysis.domain.ProcessQuantityProjection;
import java.math.BigDecimal;
import java.util.Objects;

public class ProcessEffortPerReportingPeriodDerivationVerifier {

    public boolean verify(
            ProcessAnalysisKnowledge knowledge,
            ProcessEffortPerReportingPeriodDerivation derivation) {
        Objects.requireNonNull(knowledge, "knowledge");
        Objects.requireNonNull(derivation, "derivation");

        if (!knowledge.deterministicDerivations().contains(derivation)) {
            throw new IllegalArgumentException("deterministic derivation must be declared by the supplied knowledge");
        }

        BigDecimal volumeMagnitude = quantityProjection(knownFact(knowledge, derivation.volumeFactId())).magnitude();
        BigDecimal effortPerBusinessItemMagnitude =
                quantityProjection(knownFact(knowledge, derivation.effortPerBusinessItemFactId())).magnitude();
        BigDecimal resultMagnitude = quantityProjection(knownFact(knowledge, derivation.resultFactId())).magnitude();

        BigDecimal expectedMagnitude = volumeMagnitude.multiply(effortPerBusinessItemMagnitude);

        return expectedMagnitude.compareTo(resultMagnitude) == 0;
    }

    private static ProcessKnownFact knownFact(
            ProcessAnalysisKnowledge knowledge,
            ProcessKnownFactId factId) {
        return knowledge.knownFacts().stream()
                .filter(fact -> fact.id().equals(factId))
                .findFirst()
                .orElseThrow();
    }

    private static ProcessQuantityProjection quantityProjection(ProcessKnownFact fact) {
        return (ProcessQuantityProjection) fact.computableProjection().orElseThrow();
    }
}
