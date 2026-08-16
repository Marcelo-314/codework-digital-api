package com.codeworkdigital.api.processanalysis.application;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TechnologyFitAssessmentEvaluator {

    public List<TechnologyFitAssessment> assess(ProcessUnderstanding understanding) {
        return understanding.stages().stream()
                .map(this::assessStage)
                .toList();
    }

    private TechnologyFitAssessment assessStage(ProcessUnderstandingStage stage) {
        if (stage.provenance() == ProcessStageProvenance.INFERRED) {
            return toValidate(
                    stage.id(),
                    List.of(
                            TechnologyFitReasonCode.INFERRED_STAGE,
                            TechnologyFitReasonCode.AVAILABLE_EVIDENCE_IS_INSUFFICIENT),
                    List.of(TechnologyFitValidationNeed.CONFIRM_INFERRED_STAGE));
        }

        if (stage.operationType() == ProcessStageOperationType.INTERPRET
                && stage.inputNature() == ProcessStageInputNature.UNSTRUCTURED) {
            return assessed(
                    stage.id(),
                    TechnologyFitApproach.AI_ASSISTED,
                    List.of(
                            TechnologyFitReasonCode.OBSERVED_STAGE,
                            TechnologyFitReasonCode.OPERATION_IS_INTERPRETATION,
                            TechnologyFitReasonCode.INPUT_IS_UNSTRUCTURED));
        }

        if (stage.operationType() == ProcessStageOperationType.CALCULATE
                && stage.inputNature() == ProcessStageInputNature.STRUCTURED) {
            return assessed(
                    stage.id(),
                    TechnologyFitApproach.CONVENTIONAL_SOFTWARE,
                    List.of(
                            TechnologyFitReasonCode.OBSERVED_STAGE,
                            TechnologyFitReasonCode.OPERATION_IS_CALCULATION,
                            TechnologyFitReasonCode.INPUT_IS_STRUCTURED,
                            TechnologyFitReasonCode.DETERMINISTIC_RULES_LIKELY_SUFFICIENT));
        }

        if (stage.operationType() == ProcessStageOperationType.LOOKUP
                && stage.inputNature() == ProcessStageInputNature.STRUCTURED) {
            return assessed(
                    stage.id(),
                    TechnologyFitApproach.INTEGRATION,
                    List.of(
                            TechnologyFitReasonCode.OBSERVED_STAGE,
                            TechnologyFitReasonCode.OPERATION_IS_LOOKUP,
                            TechnologyFitReasonCode.INPUT_IS_STRUCTURED,
                            TechnologyFitReasonCode.SYSTEM_ACCESS_PATTERN_SUGGESTS_INTEGRATION));
        }

        return toValidate(
                stage.id(),
                defaultReasons(stage),
                defaultValidationNeeds(stage));
    }

    private TechnologyFitAssessment assessed(
            String stageId,
            TechnologyFitApproach approach,
            List<TechnologyFitReasonCode> reasons) {
        return new TechnologyFitAssessment(stageId, approach, reasons, List.of());
    }

    private TechnologyFitAssessment toValidate(
            String stageId,
            List<TechnologyFitReasonCode> reasons,
            List<TechnologyFitValidationNeed> validationNeeds) {
        return new TechnologyFitAssessment(stageId, TechnologyFitApproach.TO_VALIDATE, reasons, validationNeeds);
    }

    private List<TechnologyFitReasonCode> defaultReasons(ProcessUnderstandingStage stage) {
        List<TechnologyFitReasonCode> reasons = new ArrayList<>();
        reasons.add(TechnologyFitReasonCode.OBSERVED_STAGE);

        if (stage.operationType() == ProcessStageOperationType.DECIDE) {
            reasons.add(TechnologyFitReasonCode.OPERATION_IS_DECISION);
        }
        if (stage.operationType() == ProcessStageOperationType.APPROVE) {
            reasons.add(TechnologyFitReasonCode.OPERATION_IS_APPROVAL);
        }

        reasons.add(TechnologyFitReasonCode.AVAILABLE_EVIDENCE_IS_INSUFFICIENT);
        return List.copyOf(reasons);
    }

    private List<TechnologyFitValidationNeed> defaultValidationNeeds(ProcessUnderstandingStage stage) {
        List<TechnologyFitValidationNeed> validationNeeds = new ArrayList<>();

        if (stage.inputNature() == ProcessStageInputNature.MIXED
                || stage.inputNature() == ProcessStageInputNature.UNKNOWN) {
            validationNeeds.add(TechnologyFitValidationNeed.CLARIFY_INPUT_STRUCTURE);
        }

        switch (stage.operationType()) {
            case DECIDE -> validationNeeds.add(TechnologyFitValidationNeed.CLARIFY_DECISION_CRITERIA);
            case APPROVE -> validationNeeds.add(TechnologyFitValidationNeed.CLARIFY_APPROVAL_AUTHORITY);
            case LOOKUP, ENTER_DATA, ROUTE, COMMUNICATE ->
                    validationNeeds.add(TechnologyFitValidationNeed.CLARIFY_SYSTEM_BOUNDARY);
            default -> validationNeeds.add(TechnologyFitValidationNeed.CLARIFY_EXECUTION_MODE);
        }

        return List.copyOf(validationNeeds);
    }
}
