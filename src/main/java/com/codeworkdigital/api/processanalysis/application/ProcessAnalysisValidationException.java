package com.codeworkdigital.api.processanalysis.application;

import jakarta.validation.ConstraintViolation;
import java.util.Set;

public class ProcessAnalysisValidationException extends RuntimeException {

    private final Set<ConstraintViolation<AnalyzeProcessDescriptionCommand>> violations;

    public ProcessAnalysisValidationException(Set<ConstraintViolation<AnalyzeProcessDescriptionCommand>> violations) {
        super("Process analysis request validation failed");
        this.violations = Set.copyOf(violations);
    }

    public Set<ConstraintViolation<AnalyzeProcessDescriptionCommand>> violations() {
        return violations;
    }
}
