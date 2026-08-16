package com.codeworkdigital.api.processanalysis.application;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ProcessAnalysisApplicationService {

    private final Validator validator;
    private final ProcessAnalysisModelClient modelClient;
    private final TechnologyFitAssessmentEvaluator technologyFitAssessmentEvaluator;

    public ProcessAnalysisApplicationService(
            Validator validator,
            ProcessAnalysisModelClient modelClient,
            TechnologyFitAssessmentEvaluator technologyFitAssessmentEvaluator) {
        this.validator = validator;
        this.modelClient = modelClient;
        this.technologyFitAssessmentEvaluator = technologyFitAssessmentEvaluator;
    }

    public ProcessUnderstanding analyze(AnalyzeProcessDescriptionCommand command) {
        validate(command);
        ProcessUnderstanding understanding = modelClient.analyze(command);
        if (!understanding.isProcessIdentified()) {
            return understanding;
        }
        return understanding.withTechnologyFitAssessments(technologyFitAssessmentEvaluator.assess(understanding));
    }

    private void validate(AnalyzeProcessDescriptionCommand command) {
        Set<ConstraintViolation<AnalyzeProcessDescriptionCommand>> violations = validator.validate(command);
        if (!violations.isEmpty()) {
            throw new ProcessAnalysisValidationException(violations);
        }
    }
}
