package com.codeworkdigital.api.processanalysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeworkdigital.api.processanalysis.domain.ProcessAnalysisScope;
import com.codeworkdigital.api.processanalysis.domain.ProcessBusinessItemPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessBusinessItemUnitId;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerBusinessItemUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerReportingPeriodDerivation;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessFactGrounding;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFact;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFactId;
import com.codeworkdigital.api.processanalysis.domain.ProcessQuantityProjection;
import com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProcessEffortMaterialityAssessmentEvaluatorTest {

    private final ProcessEffortMaterialityAssessmentEvaluator evaluator =
            new ProcessEffortMaterialityAssessmentEvaluator();

    @Test
    void noDeterministicDerivedResultIsNotEstablished() {
        ProcessEffortMaterialityAssessment assessment = evaluator.assess(Optional.empty());

        assertThat(assessment.status()).isEqualTo(ProcessEffortMaterialityAssessmentStatus.NOT_ESTABLISHED);
        assertThat(assessment.establishedOperationalBurden()).isEmpty();
    }

    @Test
    void eightMinutesPerMonthDoesNotReachP06LabMaterialityThreshold() {
        ProcessEffortMaterialityAssessment assessment = evaluator.assess(Optional.of(derivedResult("8")));

        assertThat(assessment.status())
                .isEqualTo(ProcessEffortMaterialityAssessmentStatus.NO_MATERIAL_JUSTIFICATION_IDENTIFIED);
    }

    @Test
    void eightThousandMinutesPerMonthIdentifiesOpportunity() {
        ProcessEffortMaterialityAssessment assessment = evaluator.assess(Optional.of(derivedResult("8000")));

        assertThat(assessment.status()).isEqualTo(ProcessEffortMaterialityAssessmentStatus.OPPORTUNITY_IDENTIFIED);
    }

    @Test
    void belowThresholdDecimalDoesNotReachP06LabMaterialityThreshold() {
        ProcessEffortMaterialityAssessment assessment = evaluator.assess(Optional.of(derivedResult("2399.9")));

        assertThat(assessment.status())
                .isEqualTo(ProcessEffortMaterialityAssessmentStatus.NO_MATERIAL_JUSTIFICATION_IDENTIFIED);
    }

    @Test
    void exactThresholdIdentifiesOpportunity() {
        ProcessEffortMaterialityAssessment assessment = evaluator.assess(Optional.of(derivedResult("2400")));

        assertThat(assessment.status()).isEqualTo(ProcessEffortMaterialityAssessmentStatus.OPPORTUNITY_IDENTIFIED);
    }

    @Test
    void aboveThresholdIdentifiesOpportunity() {
        ProcessEffortMaterialityAssessment assessment = evaluator.assess(Optional.of(derivedResult("2400.1")));

        assertThat(assessment.status()).isEqualTo(ProcessEffortMaterialityAssessmentStatus.OPPORTUNITY_IDENTIFIED);
    }

    @Test
    void thresholdProjectionIsExactlyTwentyFourHundredMinutesPerMonth() {
        ProcessQuantityProjection threshold = evaluator.assess(Optional.empty()).threshold().projection();

        assertThat(threshold.magnitude()).isEqualByComparingTo("2400");
        assertThat(threshold.unit()).isEqualTo(new ProcessEffortPerReportingPeriodUnit(
                ProcessEffortDurationUnit.MINUTE,
                ProcessReportingPeriodUnit.MONTH));
    }

    @Test
    void assessmentReferencesTheDerivedResultProjectionAsEstablishedBurden() {
        ProcessEffortDerivedResult derivedResult = derivedResult("8000");
        ProcessQuantityProjection projection =
                (ProcessQuantityProjection) derivedResult.resultFact().computableProjection().orElseThrow();

        ProcessEffortMaterialityAssessment assessment = evaluator.assess(Optional.of(derivedResult));

        assertThat(assessment.establishedOperationalBurden()).containsSame(projection);
    }

    @Test
    void notEstablishedWithEmptyBurdenIsValid() {
        ProcessEffortMaterialityAssessment assessment = ProcessEffortMaterialityAssessment.notEstablished(
                ProcessEffortMaterialityThreshold.P06_LAB_POLICY);

        assertThat(assessment.status()).isEqualTo(ProcessEffortMaterialityAssessmentStatus.NOT_ESTABLISHED);
        assertThat(assessment.establishedOperationalBurden()).isEmpty();
    }

    @Test
    void notEstablishedWithPresentBurdenIsRejected() {
        assertThatThrownBy(() -> new ProcessEffortMaterialityAssessment(
                ProcessEffortMaterialityAssessmentStatus.NOT_ESTABLISHED,
                ProcessEffortMaterialityThreshold.P06_LAB_POLICY,
                Optional.of(quantityProjection("8"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not carry a burden");
    }

    @Test
    void noMaterialJustificationWithMissingBurdenIsRejected() {
        assertThatThrownBy(() -> new ProcessEffortMaterialityAssessment(
                ProcessEffortMaterialityAssessmentStatus.NO_MATERIAL_JUSTIFICATION_IDENTIFIED,
                ProcessEffortMaterialityThreshold.P06_LAB_POLICY,
                Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires a burden");
    }

    @Test
    void opportunityIdentifiedWithMissingBurdenIsRejected() {
        assertThatThrownBy(() -> new ProcessEffortMaterialityAssessment(
                ProcessEffortMaterialityAssessmentStatus.OPPORTUNITY_IDENTIFIED,
                ProcessEffortMaterialityThreshold.P06_LAB_POLICY,
                Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires a burden");
    }

    @Test
    void establishedAssessmentWithExpectedMinutePerMonthBurdenIsValid() {
        ProcessEffortMaterialityAssessment assessment = new ProcessEffortMaterialityAssessment(
                ProcessEffortMaterialityAssessmentStatus.NO_MATERIAL_JUSTIFICATION_IDENTIFIED,
                ProcessEffortMaterialityThreshold.P06_LAB_POLICY,
                Optional.of(quantityProjection("8")));

        assertThat(assessment.establishedOperationalBurden()).isPresent();
    }

    @Test
    void establishedBurdenWithIncompatibleUnitIsRejected() {
        ProcessQuantityProjection incompatibleBurden = new ProcessQuantityProjection(
                new BigDecimal("8"),
                new ProcessBusinessItemPerReportingPeriodUnit(
                        new ProcessBusinessItemUnitId("item-1"),
                        ProcessReportingPeriodUnit.MONTH));

        assertThatThrownBy(() -> new ProcessEffortMaterialityAssessment(
                ProcessEffortMaterialityAssessmentStatus.NO_MATERIAL_JUSTIFICATION_IDENTIFIED,
                ProcessEffortMaterialityThreshold.P06_LAB_POLICY,
                Optional.of(incompatibleBurden)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unit must match");
    }

    @Test
    void evaluatorDoesNotRecalculateVolumeTimesEffortOrConsumeSourceOperands() {
        ProcessEffortDerivedResult derivedResult = derivedResultWithDerivationIds(
                "8",
                new ProcessKnownFactId("unavailable-volume-fact"),
                new ProcessKnownFactId("unavailable-effort-fact"));

        ProcessEffortMaterialityAssessment assessment = evaluator.assess(Optional.of(derivedResult));

        assertThat(assessment.status())
                .isEqualTo(ProcessEffortMaterialityAssessmentStatus.NO_MATERIAL_JUSTIFICATION_IDENTIFIED);
        assertThat(evaluatorPublicMethodParameterTypes())
                .containsExactly(Optional.class)
                .doesNotContain(ProcessEffortEvidence.class, ProcessEffortSourceKnowledge.class);
        assertThat(declaredFieldTypes(ProcessEffortMaterialityAssessmentEvaluator.class))
                .doesNotContain(ProcessEffortEvidence.class, ProcessEffortSourceKnowledge.class);
        assertThat(constructorParameterTypes(ProcessEffortMaterialityAssessmentEvaluator.class))
                .doesNotContain(ProcessEffortEvidence.class, ProcessEffortSourceKnowledge.class);
    }

    @Test
    void wrongOrMissingDerivedResultProjectionFailsClosedAsNotEstablished() {
        ProcessEffortDerivedResult missingProjection = derivedResult(Optional.empty());
        ProcessEffortDerivedResult wrongProjection = derivedResult(Optional.of(new ProcessQuantityProjection(
                new BigDecimal("8000"),
                new ProcessEffortPerBusinessItemUnit(
                        ProcessEffortDurationUnit.MINUTE,
                        new ProcessBusinessItemUnitId("item-1")))));

        assertThat(evaluator.assess(Optional.of(missingProjection)).status())
                .isEqualTo(ProcessEffortMaterialityAssessmentStatus.NOT_ESTABLISHED);
        assertThat(evaluator.assess(Optional.of(wrongProjection)).status())
                .isEqualTo(ProcessEffortMaterialityAssessmentStatus.NOT_ESTABLISHED);
    }

    @Test
    void nonDeterministicOperationalBurdenFactFailsClosedAsNotEstablished() {
        ProcessEffortDerivedResult wrongId = derivedResultWithResultFact(
                new ProcessKnownFact(
                        new ProcessKnownFactId("fact-other-derived-result"),
                        "wrong id",
                        ProcessFactGrounding.DETERMINISTICALLY_DERIVED,
                        ProcessAnalysisScope.processWide(),
                        List.of(
                                ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
                                ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID),
                        List.of(),
                        Optional.of(quantityProjection("8000"))));

        assertThat(evaluator.assess(Optional.of(wrongId)).status())
                .isEqualTo(ProcessEffortMaterialityAssessmentStatus.NOT_ESTABLISHED);
    }

    @Test
    void evaluatorDoesNotIntroduceFteBusinessCostOrInterventionJustificationStates() {
        assertThat(Arrays.stream(ProcessEffortMaterialityAssessmentStatus.values()).map(Enum::name))
                .containsExactly(
                        "NOT_ESTABLISHED",
                        "NO_MATERIAL_JUSTIFICATION_IDENTIFIED",
                        "OPPORTUNITY_IDENTIFIED")
                .doesNotContain("NOT_ASSESSED", "PENDING", "INTERVENTION_JUSTIFIED");
    }

    @Test
    void evaluatorDoesNotDependOnTechnologyFitOrDerivationVerifier() {
        assertThat(declaredFieldTypes(ProcessEffortMaterialityAssessmentEvaluator.class))
                .doesNotContain(
                        TechnologyFitAssessmentEvaluator.class,
                        ProcessEffortPerReportingPeriodDerivationVerifier.class);
        assertThat(constructorParameterTypes(ProcessEffortMaterialityAssessmentEvaluator.class))
                .doesNotContain(
                        TechnologyFitAssessmentEvaluator.class,
                        ProcessEffortPerReportingPeriodDerivationVerifier.class);
    }

    private ProcessEffortDerivedResult derivedResult(String magnitude) {
        return derivedResult(Optional.of(quantityProjection(magnitude)));
    }

    private ProcessEffortDerivedResult derivedResult(Optional<ProcessQuantityProjection> projection) {
        ProcessKnownFact resultFact = new ProcessKnownFact(
                ProcessEffortPerReportingPeriodMaterializer.RESULT_FACT_ID,
                "Deterministically derived effort",
                ProcessFactGrounding.DETERMINISTICALLY_DERIVED,
                ProcessAnalysisScope.processWide(),
                List.of(
                        ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
                        ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID),
                List.of(),
                projection.map(value -> value));
        return derivedResultWithResultFact(resultFact);
    }

    private ProcessEffortDerivedResult derivedResultWithDerivationIds(
            String magnitude,
            ProcessKnownFactId volumeFactId,
            ProcessKnownFactId effortFactId) {
        ProcessKnownFact resultFact = new ProcessKnownFact(
                ProcessEffortPerReportingPeriodMaterializer.RESULT_FACT_ID,
                "Deterministically derived effort",
                ProcessFactGrounding.DETERMINISTICALLY_DERIVED,
                ProcessAnalysisScope.processWide(),
                List.of(volumeFactId, effortFactId),
                List.of(),
                Optional.of(quantityProjection(magnitude)));
        return new ProcessEffortDerivedResult(
                resultFact,
                new ProcessEffortPerReportingPeriodDerivation(
                        volumeFactId,
                        effortFactId,
                        ProcessEffortPerReportingPeriodMaterializer.RESULT_FACT_ID));
    }

    private ProcessEffortDerivedResult derivedResultWithResultFact(ProcessKnownFact resultFact) {
        return new ProcessEffortDerivedResult(
                resultFact,
                new ProcessEffortPerReportingPeriodDerivation(
                        ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
                        ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID,
                        resultFact.id()));
    }

    private ProcessQuantityProjection quantityProjection(String magnitude) {
        return new ProcessQuantityProjection(
                new BigDecimal(magnitude),
                new ProcessEffortPerReportingPeriodUnit(
                        ProcessEffortDurationUnit.MINUTE,
                        ProcessReportingPeriodUnit.MONTH));
    }

    private List<Class<?>> declaredFieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .map(Field::getType)
                .toList();
    }

    private List<Class<?>> constructorParameterTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredConstructors())
                .map(Constructor::getParameterTypes)
                .flatMap(Arrays::stream)
                .toList();
    }

    private List<Class<?>> evaluatorPublicMethodParameterTypes() {
        return Arrays.stream(ProcessEffortMaterialityAssessmentEvaluator.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("assess"))
                .map(Method::getParameterTypes)
                .flatMap(Arrays::stream)
                .toList();
    }
}
