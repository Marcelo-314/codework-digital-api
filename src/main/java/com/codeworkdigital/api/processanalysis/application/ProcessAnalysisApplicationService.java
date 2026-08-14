package com.codeworkdigital.api.processanalysis.application;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ProcessAnalysisApplicationService {

    private final Validator validator;
    private final ProcessAnalysisModelClient modelClient;

    public ProcessAnalysisApplicationService(
            Validator validator,
            ProcessAnalysisModelClient modelClient) {
        this.validator = validator;
        this.modelClient = modelClient;
    }

    public ProcessUnderstanding analyze(AnalyzeProcessDescriptionCommand command) {
        validate(command);
        return modelClient.analyze(command);
    }

    private void validate(AnalyzeProcessDescriptionCommand command) {
        Set<ConstraintViolation<AnalyzeProcessDescriptionCommand>> violations = validator.validate(command);
        if (!violations.isEmpty()) {
            throw new ProcessAnalysisValidationException(violations);
        }
    }
}
