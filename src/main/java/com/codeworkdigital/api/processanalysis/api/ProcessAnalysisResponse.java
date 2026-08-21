package com.codeworkdigital.api.processanalysis.api;

import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisResult;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisStatus;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContinuationId;
import com.codeworkdigital.api.processanalysis.application.ProcessUnderstanding;
import com.codeworkdigital.api.processanalysis.application.ProcessUnderstandingStage;
import com.codeworkdigital.api.processanalysis.application.TechnologyFitAssessment;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ProcessAnalysisResponse(
        String processDescription,
        ProcessAnalysisStatus analysisStatus,
        List<String> observations,
        List<String> inferences,
        List<String> validationQuestions,
        List<ProcessUnderstandingStage> stages,
        String preliminaryAssessment,
        List<TechnologyFitAssessment> technologyFitAssessments,
        String clarificationId,
        List<ProcessAnalysisClarificationQuestionResponse> clarificationQuestions) {

    public ProcessAnalysisResponse {
        processDescription = Objects.requireNonNull(processDescription, "processDescription");
        analysisStatus = Objects.requireNonNull(analysisStatus, "analysisStatus");
        observations = List.copyOf(observations);
        inferences = List.copyOf(inferences);
        validationQuestions = List.copyOf(validationQuestions);
        stages = List.copyOf(stages);
        preliminaryAssessment = Objects.requireNonNull(preliminaryAssessment, "preliminaryAssessment");
        technologyFitAssessments = List.copyOf(technologyFitAssessments);
        clarificationQuestions = List.copyOf(clarificationQuestions);
        if (clarificationQuestions.isEmpty() != (clarificationId == null)) {
            throw new IllegalArgumentException("clarificationId must be present exactly when clarification questions exist");
        }
    }

    static ProcessAnalysisResponse from(
            ProcessAnalysisResult result,
            Optional<ProcessEffortClarificationContinuationId> clarificationId) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(clarificationId, "clarificationId");
        ProcessUnderstanding understanding = result.understanding();
        List<ProcessAnalysisClarificationQuestionResponse> clarificationQuestions = result.materialityEvidenceGaps().stream()
                .map(ProcessAnalysisClarificationQuestionResponse::from)
                .toList();
        return new ProcessAnalysisResponse(
                understanding.processDescription(),
                understanding.analysisStatus(),
                understanding.observations(),
                understanding.inferences(),
                understanding.validationQuestions(),
                understanding.stages(),
                understanding.preliminaryAssessment(),
                understanding.technologyFitAssessments(),
                clarificationId.map(id -> id.value().toString()).orElse(null),
                clarificationQuestions);
    }
}
