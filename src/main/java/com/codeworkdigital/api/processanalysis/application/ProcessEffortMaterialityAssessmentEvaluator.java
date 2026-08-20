package com.codeworkdigital.api.processanalysis.application;

import com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessFactGrounding;
import com.codeworkdigital.api.processanalysis.domain.ProcessQuantityProjection;
import com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ProcessEffortMaterialityAssessmentEvaluator {

    private final ProcessEffortMaterialityThreshold threshold;

    public ProcessEffortMaterialityAssessmentEvaluator() {
        this(ProcessEffortMaterialityThreshold.P06_LAB_POLICY);
    }

    ProcessEffortMaterialityAssessmentEvaluator(ProcessEffortMaterialityThreshold threshold) {
        this.threshold = Objects.requireNonNull(threshold, "threshold");
    }

    public ProcessEffortMaterialityAssessment assess(Optional<ProcessEffortDerivedResult> derivedResult) {
        Objects.requireNonNull(derivedResult, "derivedResult");
        Optional<ProcessQuantityProjection> burden = derivedResult.flatMap(this::admissibleOperationalBurden);
        if (burden.isEmpty()) {
            return ProcessEffortMaterialityAssessment.notEstablished(threshold);
        }

        ProcessEffortMaterialityAssessmentStatus status =
                burden.get().magnitude().compareTo(threshold.projection().magnitude()) < 0
                        ? ProcessEffortMaterialityAssessmentStatus.NO_MATERIAL_JUSTIFICATION_IDENTIFIED
                        : ProcessEffortMaterialityAssessmentStatus.OPPORTUNITY_IDENTIFIED;
        return new ProcessEffortMaterialityAssessment(status, threshold, burden);
    }

    private Optional<ProcessQuantityProjection> admissibleOperationalBurden(ProcessEffortDerivedResult derivedResult) {
        if (!derivedResult.resultFact().id().equals(ProcessEffortPerReportingPeriodMaterializer.RESULT_FACT_ID)
                || derivedResult.resultFact().grounding() != ProcessFactGrounding.DETERMINISTICALLY_DERIVED) {
            return Optional.empty();
        }
        return derivedResult.resultFact().computableProjection()
                .filter(ProcessQuantityProjection.class::isInstance)
                .map(ProcessQuantityProjection.class::cast)
                .filter(ProcessEffortMaterialityAssessmentEvaluator::isMinutePerMonth);
    }

    private static boolean isMinutePerMonth(ProcessQuantityProjection projection) {
        return projection.unit() instanceof ProcessEffortPerReportingPeriodUnit unit
                && unit.effortDuration() == ProcessEffortDurationUnit.MINUTE
                && unit.reportingPeriod() == ProcessReportingPeriodUnit.MONTH;
    }
}
