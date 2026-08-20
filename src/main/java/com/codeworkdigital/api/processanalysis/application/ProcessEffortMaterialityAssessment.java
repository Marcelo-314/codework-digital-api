package com.codeworkdigital.api.processanalysis.application;

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
    }

    public static ProcessEffortMaterialityAssessment notEstablished(
            ProcessEffortMaterialityThreshold threshold) {
        return new ProcessEffortMaterialityAssessment(
                ProcessEffortMaterialityAssessmentStatus.NOT_ESTABLISHED,
                threshold,
                Optional.empty());
    }
}
