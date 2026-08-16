package com.codeworkdigital.api.processanalysis.application;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record ProcessUnderstanding(
        String processDescription,
        List<String> observations,
        List<String> inferences,
        List<String> validationQuestions,
        List<ProcessUnderstandingStage> stages,
        String preliminaryAssessment,
        List<TechnologyFitAssessment> technologyFitAssessments) {

    public ProcessUnderstanding(
            String processDescription,
            List<String> observations,
            List<String> inferences,
            List<String> validationQuestions,
            List<ProcessUnderstandingStage> stages,
            String preliminaryAssessment) {
        this(
                processDescription,
                observations,
                inferences,
                validationQuestions,
                stages,
                preliminaryAssessment,
                List.of());
    }

    public ProcessUnderstanding {
        observations = List.copyOf(observations);
        inferences = List.copyOf(inferences);
        validationQuestions = List.copyOf(validationQuestions);
        stages = List.copyOf(stages);
        technologyFitAssessments = List.copyOf(technologyFitAssessments);
        validateTechnologyFitReferences(stages, technologyFitAssessments);
    }

    public ProcessUnderstanding withTechnologyFitAssessments(List<TechnologyFitAssessment> assessments) {
        return new ProcessUnderstanding(
                processDescription,
                observations,
                inferences,
                validationQuestions,
                stages,
                preliminaryAssessment,
                assessments);
    }

    private static void validateTechnologyFitReferences(
            List<ProcessUnderstandingStage> stages,
            List<TechnologyFitAssessment> technologyFitAssessments) {
        Set<String> stageIds = new HashSet<>();
        for (ProcessUnderstandingStage stage : stages) {
            stageIds.add(stage.id());
        }

        Set<String> referencedStageIds = new HashSet<>();
        for (TechnologyFitAssessment assessment : technologyFitAssessments) {
            if (!stageIds.contains(assessment.sourceStageId())) {
                throw new IllegalArgumentException("Technology fit assessment references an unknown stage");
            }
            if (!referencedStageIds.add(assessment.sourceStageId())) {
                throw new IllegalArgumentException("Technology fit assessments must not repeat source stages");
            }
        }
    }
}
