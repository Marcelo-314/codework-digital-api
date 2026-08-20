package com.codeworkdigital.api.processanalysis.application;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ProcessAnalysisApplicationService {

    private final Validator validator;
    private final ProcessAnalysisModelClient modelClient;
    private final ProcessEffortEvidenceProjectionMapper effortEvidenceProjectionMapper;
    private final ProcessEffortPerReportingPeriodMaterializer effortPerReportingPeriodMaterializer;
    private final ProcessEffortMaterialityAssessmentEvaluator effortMaterialityAssessmentEvaluator;
    private final TechnologyFitAssessmentEvaluator technologyFitAssessmentEvaluator;

    public ProcessAnalysisApplicationService(
            Validator validator,
            ProcessAnalysisModelClient modelClient,
            ProcessEffortEvidenceProjectionMapper effortEvidenceProjectionMapper,
            ProcessEffortPerReportingPeriodMaterializer effortPerReportingPeriodMaterializer,
            ProcessEffortMaterialityAssessmentEvaluator effortMaterialityAssessmentEvaluator,
            TechnologyFitAssessmentEvaluator technologyFitAssessmentEvaluator) {
        this.validator = validator;
        this.modelClient = modelClient;
        this.effortEvidenceProjectionMapper = effortEvidenceProjectionMapper;
        this.effortPerReportingPeriodMaterializer = effortPerReportingPeriodMaterializer;
        this.effortMaterialityAssessmentEvaluator = effortMaterialityAssessmentEvaluator;
        this.technologyFitAssessmentEvaluator = technologyFitAssessmentEvaluator;
    }

    public ProcessAnalysisResult analyze(AnalyzeProcessDescriptionCommand command) {
        validate(command);
        ProcessAnalysisModelResult modelResult = modelClient.analyze(command);
        ProcessUnderstanding understanding = modelResult.understanding();
        if (!understanding.isProcessIdentified()) {
            return mapAndMaterialize(new ProcessAnalysisModelResult(
                    understanding,
                    ProcessEffortEvidence.empty()));
        }
        ProcessUnderstanding assessedUnderstanding =
                understanding.withTechnologyFitAssessments(technologyFitAssessmentEvaluator.assess(understanding));
        return mapAndMaterialize(new ProcessAnalysisModelResult(
                assessedUnderstanding,
                modelResult.effortEvidence()));
    }

    private ProcessAnalysisResult mapAndMaterialize(ProcessAnalysisModelResult modelResult) {
        ProcessAnalysisResult result = effortEvidenceProjectionMapper.map(modelResult);
        ProcessAnalysisResult materialized =
                result.withDerivedResult(effortPerReportingPeriodMaterializer.materialize(result.sourceKnowledge()));
        return materialized.withMaterialityAssessment(
                effortMaterialityAssessmentEvaluator.assess(materialized.derivedResult()));
    }

    private void validate(AnalyzeProcessDescriptionCommand command) {
        Set<ConstraintViolation<AnalyzeProcessDescriptionCommand>> violations = validator.validate(command);
        if (!violations.isEmpty()) {
            throw new ProcessAnalysisValidationException(violations);
        }
    }
}
