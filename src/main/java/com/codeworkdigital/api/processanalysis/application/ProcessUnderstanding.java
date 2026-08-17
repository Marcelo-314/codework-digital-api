package com.codeworkdigital.api.processanalysis.application;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ProcessUnderstanding(
        String processDescription,
        ProcessAnalysisStatus analysisStatus,
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
                ProcessAnalysisStatus.PROCESS_IDENTIFIED,
                observations,
                inferences,
                validationQuestions,
                stages,
                preliminaryAssessment,
                List.of());
    }

    public ProcessUnderstanding(
            String processDescription,
            ProcessAnalysisStatus analysisStatus,
            List<String> observations,
            List<String> inferences,
            List<String> validationQuestions,
            List<ProcessUnderstandingStage> stages,
            String preliminaryAssessment) {
        this(
                processDescription,
                analysisStatus,
                observations,
                inferences,
                validationQuestions,
                stages,
                preliminaryAssessment,
                List.of());
    }

    public ProcessUnderstanding {
        processDescription = Objects.requireNonNull(processDescription, "processDescription");
        analysisStatus = Objects.requireNonNull(analysisStatus, "analysisStatus");
        observations = List.copyOf(observations);
        inferences = List.copyOf(inferences);
        validationQuestions = List.copyOf(validationQuestions);
        stages = List.copyOf(stages);
        preliminaryAssessment = Objects.requireNonNull(preliminaryAssessment, "preliminaryAssessment");
        technologyFitAssessments = List.copyOf(technologyFitAssessments);
        validateTechnologyFitState(analysisStatus, technologyFitAssessments);
        validateTechnologyFitReferences(stages, technologyFitAssessments);
    }

    public ProcessUnderstanding withTechnologyFitAssessments(List<TechnologyFitAssessment> assessments) {
        return new ProcessUnderstanding(
                processDescription,
                analysisStatus,
                observations,
                inferences,
                validationQuestions,
                stages,
                preliminaryAssessment,
                assessments);
    }

    @JsonIgnore
    public boolean isProcessIdentified() {
        return analysisStatus == ProcessAnalysisStatus.PROCESS_IDENTIFIED;
    }

    private static void validateTechnologyFitState(
            ProcessAnalysisStatus analysisStatus,
            List<TechnologyFitAssessment> technologyFitAssessments) {
        if (analysisStatus != ProcessAnalysisStatus.PROCESS_IDENTIFIED && !technologyFitAssessments.isEmpty()) {
            throw new IllegalArgumentException(
                    "Technology fit assessments are only allowed when a process was identified");
        }
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
