package com.codeworkdigital.api.processanalysis.api;

import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContinuationId;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationResolution;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortReasoningProjector;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessQuantityProjection;
import com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit;
import java.math.BigDecimal;
import java.util.Objects;

public record ProcessEffortClarificationResolutionResponse(
        String clarificationId,
        OperationalBurden operationalBurden,
        ProcessEffortMaterialityOutcomeResponse materialityOutcome,
        ProcessEffortReasoningResponse reasoning) {

    public ProcessEffortClarificationResolutionResponse {
        clarificationId = Objects.requireNonNull(clarificationId, "clarificationId");
        operationalBurden = Objects.requireNonNull(operationalBurden, "operationalBurden");
        materialityOutcome = Objects.requireNonNull(materialityOutcome, "materialityOutcome");
        reasoning = Objects.requireNonNull(reasoning, "reasoning");
    }

    static ProcessEffortClarificationResolutionResponse from(
            ProcessEffortClarificationContinuationId clarificationId,
            ProcessEffortClarificationResolution resolution) {
        Objects.requireNonNull(clarificationId, "clarificationId");
        Objects.requireNonNull(resolution, "resolution");
        ProcessQuantityProjection burden = resolution.materialityAssessment()
                .establishedOperationalBurden()
                .orElseThrow(() -> new IllegalStateException("successful clarification response requires burden"));
        if (!(burden.unit() instanceof ProcessEffortPerReportingPeriodUnit unit)
                || unit.effortDuration() != ProcessEffortDurationUnit.MINUTE
                || unit.reportingPeriod() != ProcessReportingPeriodUnit.MONTH) {
            throw new IllegalStateException("successful clarification response requires minute per month burden");
        }
        return new ProcessEffortClarificationResolutionResponse(
                clarificationId.value().toString(),
                new OperationalBurden(burden.magnitude(), "MINUTE_PER_MONTH"),
                ProcessEffortMaterialityOutcomeResponse.from(resolution.materialityAssessment().status()),
                ProcessEffortReasoningResponse.from(ProcessEffortReasoningProjector.project(resolution)));
    }

    public record OperationalBurden(
            BigDecimal magnitude,
            String unit) {

        public OperationalBurden {
            magnitude = Objects.requireNonNull(magnitude, "magnitude");
            unit = Objects.requireNonNull(unit, "unit");
        }
    }
}
