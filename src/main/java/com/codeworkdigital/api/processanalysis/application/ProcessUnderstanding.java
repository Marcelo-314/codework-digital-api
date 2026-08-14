package com.codeworkdigital.api.processanalysis.application;

import java.util.List;

public record ProcessUnderstanding(
        String processDescription,
        List<String> observations,
        List<String> inferences,
        List<String> validationQuestions,
        List<ProcessUnderstandingStage> stages,
        String preliminaryAssessment) {

    public ProcessUnderstanding {
        observations = List.copyOf(observations);
        inferences = List.copyOf(inferences);
        validationQuestions = List.copyOf(validationQuestions);
        stages = List.copyOf(stages);
    }
}
