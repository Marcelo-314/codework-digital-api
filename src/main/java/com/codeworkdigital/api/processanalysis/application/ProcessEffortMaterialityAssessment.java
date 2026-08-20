package com.codeworkdigital.api.processanalysis.application;

import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessQuantityProjection;
import java.util.Objects;
import java.util.Optional;

public record ProcessEffortMaterialityAssessment(
        ProcessEffortMaterialityAssessmentStatus status,
        ProcessEffortMaterialityThreshold threshold,
        Optional<ProcessQuantityProjection> establishedOperationalBurden) {

    public ProcessEffortMaterialityAssessment {
        status = Objects.requireNonNull(status, "status");
        threshold = Objects.requireNonNull(threshold, "threshold");
        establishedOperationalBurden =
                Objects.requireNonNull(establishedOperationalBurden, "establishedOperationalBurden");
        validateBurdenPresence(status, establishedOperationalBurden);
        if (establishedOperationalBurden.isPresent()) {
            validateBurdenUnit(threshold, establishedOperationalBurden.orElseThrow());
        }
    }

    public static ProcessEffortMaterialityAssessment notEstablished(
            ProcessEffortMaterialityThreshold threshold) {
        return new ProcessEffortMaterialityAssessment(
                ProcessEffortMaterialityAssessmentStatus.NOT_ESTABLISHED,
                threshold,
                Optional.empty());
    }

    private static void validateBurdenPresence(
            ProcessEffortMaterialityAssessmentStatus status,
            Optional<ProcessQuantityProjection> establishedOperationalBurden) {
        if (status == ProcessEffortMaterialityAssessmentStatus.NOT_ESTABLISHED
                && establishedOperationalBurden.isPresent()) {
            throw new IllegalArgumentException("NOT_ESTABLISHED materiality assessment must not carry a burden");
        }
        if (status != ProcessEffortMaterialityAssessmentStatus.NOT_ESTABLISHED
                && establishedOperationalBurden.isEmpty()) {
            throw new IllegalArgumentException("established materiality assessment requires a burden");
        }
    }

    private static void validateBurdenUnit(
            ProcessEffortMaterialityThreshold threshold,
            ProcessQuantityProjection burden) {
        if (!threshold.projection().unit().equals(burden.unit())
                || !(burden.unit() instanceof ProcessEffortPerReportingPeriodUnit)) {
            throw new IllegalArgumentException(
                    "established materiality burden unit must match the threshold unit");
        }
    }
}
