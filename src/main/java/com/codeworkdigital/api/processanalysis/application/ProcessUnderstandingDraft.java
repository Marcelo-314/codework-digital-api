package com.codeworkdigital.api.processanalysis.application;

import java.util.List;

public record ProcessUnderstandingDraft(
        List<String> observations,
        List<String> inferences,
        List<String> validationQuestions,
        List<ProcessUnderstandingStage> stages,
        String preliminaryAssessment) {
}
