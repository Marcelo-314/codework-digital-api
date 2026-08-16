package com.codeworkdigital.api.processanalysis.application;

import java.util.List;

public record TechnologyFitAssessment(
        String sourceStageId,
        TechnologyFitApproach approach,
        List<TechnologyFitReasonCode> reasons,
        List<TechnologyFitValidationNeed> validationNeeds) {

    public TechnologyFitAssessment {
        if (sourceStageId == null || sourceStageId.isBlank()) {
            throw new IllegalArgumentException("Technology fit assessment requires a source stage id");
        }
        if (approach == null) {
            throw new IllegalArgumentException("Technology fit assessment requires an approach");
        }

        reasons = List.copyOf(reasons);
        validationNeeds = List.copyOf(validationNeeds);

        if (reasons.isEmpty() || reasons.stream().anyMatch(reason -> reason == null)) {
            throw new IllegalArgumentException("Technology fit assessment requires non-null rationale codes");
        }
        if (validationNeeds.stream().anyMatch(validationNeed -> validationNeed == null)) {
            throw new IllegalArgumentException("Technology fit assessment validation needs must be non-null");
        }
        if (approach == TechnologyFitApproach.TO_VALIDATE && validationNeeds.isEmpty()) {
            throw new IllegalArgumentException("TO_VALIDATE assessments require validation needs");
        }
        if (approach != TechnologyFitApproach.TO_VALIDATE && !validationNeeds.isEmpty()) {
            throw new IllegalArgumentException("Only TO_VALIDATE assessments can declare validation needs");
        }
    }
}
