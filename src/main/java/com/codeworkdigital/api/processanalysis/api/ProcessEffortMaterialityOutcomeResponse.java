package com.codeworkdigital.api.processanalysis.api;

import com.codeworkdigital.api.processanalysis.application.ProcessEffortMaterialityAssessmentStatus;

public enum ProcessEffortMaterialityOutcomeResponse {
    NO_MATERIAL_JUSTIFICATION_IDENTIFIED,
    OPPORTUNITY_IDENTIFIED;

    static ProcessEffortMaterialityOutcomeResponse from(ProcessEffortMaterialityAssessmentStatus status) {
        return switch (status) {
            case NO_MATERIAL_JUSTIFICATION_IDENTIFIED -> NO_MATERIAL_JUSTIFICATION_IDENTIFIED;
            case OPPORTUNITY_IDENTIFIED -> OPPORTUNITY_IDENTIFIED;
            case NOT_ESTABLISHED -> throw new IllegalStateException(
                    "not established has no public materiality outcome");
        };
    }
}
