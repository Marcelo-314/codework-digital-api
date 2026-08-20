package com.codeworkdigital.api.processanalysis.application;

import com.codeworkdigital.api.processanalysis.domain.ProcessAnalysisScope;
import com.codeworkdigital.api.processanalysis.domain.ProcessBusinessItemPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerBusinessItemUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerReportingPeriodDerivation;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessFactGrounding;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFact;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFactId;
import com.codeworkdigital.api.processanalysis.domain.ProcessQuantityProjection;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ProcessEffortPerReportingPeriodMaterializer {

    static final ProcessKnownFactId RESULT_FACT_ID =
            new ProcessKnownFactId("fact-effort-per-reporting-period");

    public Optional<ProcessEffortDerivedResult> materialize(ProcessEffortSourceKnowledge sourceKnowledge) {
        Objects.requireNonNull(sourceKnowledge, "sourceKnowledge");
        Optional<ProcessKnownFact> volumeFact = sourceFact(
                sourceKnowledge,
                ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID);
        Optional<ProcessKnownFact> effortFact = sourceFact(
                sourceKnowledge,
                ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID);
        if (volumeFact.isEmpty() || effortFact.isEmpty()) {
            return Optional.empty();
        }

        Optional<ProcessQuantityProjection> volumeProjection = quantityProjection(volumeFact.get());
        Optional<ProcessQuantityProjection> effortProjection = quantityProjection(effortFact.get());
        if (volumeProjection.isEmpty() || effortProjection.isEmpty()) {
            return Optional.empty();
        }
        if (!(volumeProjection.get().unit() instanceof ProcessBusinessItemPerReportingPeriodUnit volumeUnit)
                || !(effortProjection.get().unit() instanceof ProcessEffortPerBusinessItemUnit effortUnit)) {
            return Optional.empty();
        }
        if (!volumeUnit.businessItemId().equals(effortUnit.businessItemId())) {
            return Optional.empty();
        }

        BigDecimal resultMagnitude = volumeProjection.get().magnitude().multiply(effortProjection.get().magnitude());
        ProcessQuantityProjection resultProjection = new ProcessQuantityProjection(
                resultMagnitude,
                new ProcessEffortPerReportingPeriodUnit(
                        effortUnit.effortDuration(),
                        volumeUnit.reportingPeriod()));
        ProcessKnownFact resultFact = new ProcessKnownFact(
                RESULT_FACT_ID,
                statement(resultProjection),
                ProcessFactGrounding.DETERMINISTICALLY_DERIVED,
                ProcessAnalysisScope.processWide(),
                List.of(
                        ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
                        ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID),
                List.of(),
                Optional.of(resultProjection));
        ProcessEffortPerReportingPeriodDerivation derivation = new ProcessEffortPerReportingPeriodDerivation(
                ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
                ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID,
                RESULT_FACT_ID);
        return Optional.of(new ProcessEffortDerivedResult(resultFact, derivation));
    }

    private static Optional<ProcessKnownFact> sourceFact(
            ProcessEffortSourceKnowledge sourceKnowledge,
            ProcessKnownFactId factId) {
        return sourceKnowledge.knownFacts().stream()
                .filter(fact -> fact.id().equals(factId))
                .filter(fact -> fact.grounding() == ProcessFactGrounding.SOURCE_STATED)
                .findFirst();
    }

    private static Optional<ProcessQuantityProjection> quantityProjection(ProcessKnownFact fact) {
        return fact.computableProjection()
                .filter(ProcessQuantityProjection.class::isInstance)
                .map(ProcessQuantityProjection.class::cast);
    }

    private static String statement(ProcessQuantityProjection projection) {
        ProcessEffortPerReportingPeriodUnit unit = (ProcessEffortPerReportingPeriodUnit) projection.unit();
        return "Deterministically derived effort: %s %s per %s"
                .formatted(
                        projection.magnitude().toPlainString(),
                        unit.effortDuration().name().toLowerCase(),
                        unit.reportingPeriod().name().toLowerCase());
    }
}
