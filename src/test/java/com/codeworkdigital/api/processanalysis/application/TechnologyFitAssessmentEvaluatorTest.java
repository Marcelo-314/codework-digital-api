package com.codeworkdigital.api.processanalysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class TechnologyFitAssessmentEvaluatorTest {

    private final TechnologyFitAssessmentEvaluator evaluator = new TechnologyFitAssessmentEvaluator();

    @Test
    void interpretUnstructuredObservedStageCanBeClassifiedAsAiAssisted() {
        List<TechnologyFitAssessment> assessments = evaluator.assess(understandingWith(stage(
                "interpret-request",
                ProcessStageProvenance.OBSERVED,
                ProcessStageOperationType.INTERPRET,
                ProcessStageInputNature.UNSTRUCTURED)));

        assertThat(assessments)
                .singleElement()
                .satisfies(assessment -> {
                    assertThat(assessment.sourceStageId()).isEqualTo("interpret-request");
                    assertThat(assessment.approach()).isEqualTo(TechnologyFitApproach.AI_ASSISTED);
                    assertThat(assessment.reasons()).containsExactly(
                            TechnologyFitReasonCode.OBSERVED_STAGE,
                            TechnologyFitReasonCode.OPERATION_IS_INTERPRETATION,
                            TechnologyFitReasonCode.INPUT_IS_UNSTRUCTURED);
                    assertThat(assessment.validationNeeds()).isEmpty();
                });
    }

    @Test
    void calculateStructuredObservedStageCanBeClassifiedAsConventionalSoftware() {
        List<TechnologyFitAssessment> assessments = evaluator.assess(understandingWith(stage(
                "calculate-price",
                ProcessStageProvenance.OBSERVED,
                ProcessStageOperationType.CALCULATE,
                ProcessStageInputNature.STRUCTURED)));

        assertThat(assessments)
                .singleElement()
                .satisfies(assessment -> {
                    assertThat(assessment.approach()).isEqualTo(TechnologyFitApproach.CONVENTIONAL_SOFTWARE);
                    assertThat(assessment.reasons()).contains(
                            TechnologyFitReasonCode.OPERATION_IS_CALCULATION,
                            TechnologyFitReasonCode.INPUT_IS_STRUCTURED,
                            TechnologyFitReasonCode.DETERMINISTIC_RULES_LIKELY_SUFFICIENT);
                });
    }

    @Test
    void lookupStructuredObservedStageCanBeClassifiedAsIntegration() {
        List<TechnologyFitAssessment> assessments = evaluator.assess(understandingWith(stage(
                "lookup-stock",
                ProcessStageProvenance.OBSERVED,
                ProcessStageOperationType.LOOKUP,
                ProcessStageInputNature.STRUCTURED)));

        assertThat(assessments)
                .singleElement()
                .satisfies(assessment -> {
                    assertThat(assessment.approach()).isEqualTo(TechnologyFitApproach.INTEGRATION);
                    assertThat(assessment.reasons()).contains(
                            TechnologyFitReasonCode.OPERATION_IS_LOOKUP,
                            TechnologyFitReasonCode.INPUT_IS_STRUCTURED,
                            TechnologyFitReasonCode.SYSTEM_ACCESS_PATTERN_SUGGESTS_INTEGRATION);
                });
    }

    @Test
    void ambiguousEvidenceFallsBackToValidate() {
        List<TechnologyFitAssessment> assessments = evaluator.assess(understandingWith(stage(
                "validate-case",
                ProcessStageProvenance.OBSERVED,
                ProcessStageOperationType.VALIDATE,
                ProcessStageInputNature.MIXED)));

        assertThat(assessments)
                .singleElement()
                .satisfies(assessment -> {
                    assertThat(assessment.approach()).isEqualTo(TechnologyFitApproach.TO_VALIDATE);
                    assertThat(assessment.reasons()).contains(TechnologyFitReasonCode.AVAILABLE_EVIDENCE_IS_INSUFFICIENT);
                    assertThat(assessment.validationNeeds()).contains(
                            TechnologyFitValidationNeed.CLARIFY_INPUT_STRUCTURE,
                            TechnologyFitValidationNeed.CLARIFY_EXECUTION_MODE);
                });
    }

    @Test
    void decideDoesNotAutomaticallyBecomeAiAssisted() {
        List<TechnologyFitAssessment> assessments = evaluator.assess(understandingWith(stage(
                "decide-credit",
                ProcessStageProvenance.OBSERVED,
                ProcessStageOperationType.DECIDE,
                ProcessStageInputNature.STRUCTURED)));

        assertThat(assessments)
                .singleElement()
                .satisfies(assessment -> {
                    assertThat(assessment.approach()).isEqualTo(TechnologyFitApproach.TO_VALIDATE);
                    assertThat(assessment.reasons()).contains(
                            TechnologyFitReasonCode.OPERATION_IS_DECISION,
                            TechnologyFitReasonCode.AVAILABLE_EVIDENCE_IS_INSUFFICIENT);
                    assertThat(assessment.validationNeeds())
                            .containsExactly(TechnologyFitValidationNeed.CLARIFY_DECISION_CRITERIA);
                });
    }

    @Test
    void approveDoesNotAutomaticallyBecomeHumanDecision() {
        List<TechnologyFitAssessment> assessments = evaluator.assess(understandingWith(stage(
                "approve-exception",
                ProcessStageProvenance.OBSERVED,
                ProcessStageOperationType.APPROVE,
                ProcessStageInputNature.UNKNOWN)));

        assertThat(assessments)
                .singleElement()
                .satisfies(assessment -> {
                    assertThat(assessment.approach()).isEqualTo(TechnologyFitApproach.TO_VALIDATE);
                    assertThat(assessment.validationNeeds()).contains(
                            TechnologyFitValidationNeed.CLARIFY_INPUT_STRUCTURE,
                            TechnologyFitValidationNeed.CLARIFY_APPROVAL_AUTHORITY);
                });
    }

    @Test
    void inferredStagesRemainToValidateEvenWhenOperationLooksAutomatable() {
        List<TechnologyFitAssessment> assessments = evaluator.assess(understandingWith(stage(
                "calculate-margin",
                ProcessStageProvenance.INFERRED,
                ProcessStageOperationType.CALCULATE,
                ProcessStageInputNature.STRUCTURED)));

        assertThat(assessments)
                .singleElement()
                .satisfies(assessment -> {
                    assertThat(assessment.approach()).isEqualTo(TechnologyFitApproach.TO_VALIDATE);
                    assertThat(assessment.reasons()).containsExactly(
                            TechnologyFitReasonCode.INFERRED_STAGE,
                            TechnologyFitReasonCode.AVAILABLE_EVIDENCE_IS_INSUFFICIENT);
                    assertThat(assessment.validationNeeds())
                            .containsExactly(TechnologyFitValidationNeed.CONFIRM_INFERRED_STAGE);
                });
    }

    @Test
    void evaluatorIsDeterministicForTheSameUnderstanding() {
        ProcessUnderstanding understanding = understandingWith(
                stage(
                        "interpret-request",
                        ProcessStageProvenance.OBSERVED,
                        ProcessStageOperationType.INTERPRET,
                        ProcessStageInputNature.UNSTRUCTURED),
                stage(
                        "decide-credit",
                        ProcessStageProvenance.OBSERVED,
                        ProcessStageOperationType.DECIDE,
                        ProcessStageInputNature.STRUCTURED));

        assertThat(evaluator.assess(understanding)).isEqualTo(evaluator.assess(understanding));
    }

    @Test
    void everyAssessmentReferencesAnExistingStage() {
        ProcessUnderstanding understanding = understandingWith(
                stage(
                        "interpret-request",
                        ProcessStageProvenance.OBSERVED,
                        ProcessStageOperationType.INTERPRET,
                        ProcessStageInputNature.UNSTRUCTURED),
                stage(
                        "lookup-stock",
                        ProcessStageProvenance.OBSERVED,
                        ProcessStageOperationType.LOOKUP,
                        ProcessStageInputNature.STRUCTURED));

        List<String> stageIds = understanding.stages().stream().map(ProcessUnderstandingStage::id).toList();

        assertThat(evaluator.assess(understanding))
                .allSatisfy(assessment -> assertThat(stageIds).contains(assessment.sourceStageId()));
    }

    private ProcessUnderstanding understandingWith(ProcessUnderstandingStage... stages) {
        return new ProcessUnderstanding(
                "Synthetic process description.",
                List.of("Observed detail."),
                List.of("Inferred detail."),
                List.of("Validation question."),
                List.of(stages),
                "Preliminary assessment.");
    }

    private ProcessUnderstandingStage stage(
            String id,
            ProcessStageProvenance provenance,
            ProcessStageOperationType operationType,
            ProcessStageInputNature inputNature) {
        return new ProcessUnderstandingStage(
                id,
                "Stage " + id,
                "Description for " + id + ".",
                provenance,
                operationType,
                inputNature);
    }
}
