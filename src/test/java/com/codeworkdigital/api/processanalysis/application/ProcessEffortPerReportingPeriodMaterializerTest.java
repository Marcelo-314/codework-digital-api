package com.codeworkdigital.api.processanalysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.codeworkdigital.api.processanalysis.domain.ProcessAnalysisKnowledge;
import com.codeworkdigital.api.processanalysis.domain.ProcessAnalysisScope;
import com.codeworkdigital.api.processanalysis.domain.ProcessBusinessItemPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessBusinessItemUnitId;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerBusinessItemUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessFactGrounding;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFact;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFactId;
import com.codeworkdigital.api.processanalysis.domain.ProcessQuantityProjection;
import com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProcessEffortPerReportingPeriodMaterializerTest {

    private final ProcessEffortEvidenceProjectionMapper sourceMapper = new ProcessEffortEvidenceProjectionMapper();
    private final ProcessEffortPerReportingPeriodMaterializer materializer =
            new ProcessEffortPerReportingPeriodMaterializer();

    @Test
    void materializesFourThousandRequestsAtTwoMinutesToEightThousandMinutesPerMonth() {
        ProcessEffortDerivedResult result = materialize(sourceKnowledge("4000", "2", "item-1", "item-1"));

        assertThat(quantityProjection(result).magnitude()).isEqualByComparingTo("8000");
    }

    @Test
    void materializesFourRequestsAtThreeMinutesToTwelveMinutesPerMonth() {
        ProcessEffortDerivedResult result = materialize(sourceKnowledge("4", "3", "item-1", "item-1"));

        assertThat(quantityProjection(result).magnitude()).isEqualByComparingTo("12");
    }

    @Test
    void zeroVolumeProducesZeroResult() {
        ProcessEffortDerivedResult result = materialize(sourceKnowledge("0", "2", "item-1", "item-1"));

        assertThat(quantityProjection(result).magnitude()).isEqualByComparingTo("0");
    }

    @Test
    void zeroEffortProducesZeroResult() {
        ProcessEffortDerivedResult result = materialize(sourceKnowledge("4000", "0", "item-1", "item-1"));

        assertThat(quantityProjection(result).magnitude()).isEqualByComparingTo("0");
    }

    @Test
    void decimalEffortProducesDecimalResult() {
        ProcessEffortDerivedResult result = materialize(sourceKnowledge("4", "2.4", "item-1", "item-1"));

        assertThat(quantityProjection(result).magnitude()).isEqualByComparingTo("9.6");
    }

    @Test
    void derivedFactCarriesDeterministicP06ResultShape() {
        ProcessEffortDerivedResult result = materialize(sourceKnowledge("4", "3", "item-1", "item-1"));
        ProcessKnownFact fact = result.resultFact();
        ProcessQuantityProjection projection = quantityProjection(result);

        assertThat(fact.id()).isEqualTo(ProcessEffortPerReportingPeriodMaterializer.RESULT_FACT_ID);
        assertThat(fact.grounding()).isEqualTo(ProcessFactGrounding.DETERMINISTICALLY_DERIVED);
        assertThat(fact.scope().isProcessWide()).isTrue();
        assertThat(fact.premiseFactIds()).containsExactly(
                ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
                ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID);
        assertThat(fact.evidenceArtifactIds()).isEmpty();
        assertThat(fact.computableProjection()).contains(projection);
        assertThat(projection.unit()).isInstanceOfSatisfying(
                ProcessEffortPerReportingPeriodUnit.class,
                unit -> {
                    assertThat(unit.reportingPeriod()).isEqualTo(ProcessReportingPeriodUnit.MONTH);
                    assertThat(unit.effortDuration()).isEqualTo(ProcessEffortDurationUnit.MINUTE);
                });
        assertThat(fact.statement()).isEqualTo("Deterministically derived effort: 12 minute per month");
        assertThat(fact.statement()).contains("12", "minute", "month");
    }

    @Test
    void declaresP06EffortPerReportingPeriodDerivation() {
        ProcessEffortDerivedResult result = materialize(sourceKnowledge("4", "3", "item-1", "item-1"));

        assertThat(result.derivation().volumeFactId())
                .isEqualTo(ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID);
        assertThat(result.derivation().effortPerBusinessItemFactId())
                .isEqualTo(ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID);
        assertThat(result.derivation().resultFactId())
                .isEqualTo(ProcessEffortPerReportingPeriodMaterializer.RESULT_FACT_ID);
    }

    @Test
    void sourceFactsResultFactAndDerivationConstructValidAnalysisKnowledge() {
        ProcessEffortSourceKnowledge sourceKnowledge = sourceKnowledge("4", "3", "item-1", "item-1");
        ProcessEffortDerivedResult result = materialize(sourceKnowledge);

        assertThatCode(() -> new ProcessAnalysisKnowledge(
                List.of(
                        sourceKnowledge.knownFacts().get(0),
                        sourceKnowledge.knownFacts().get(1),
                        result.resultFact()),
                List.of(),
                List.of(),
                List.of(),
                List.of(result.derivation())))
                .doesNotThrowAnyException();
    }

    @Test
    void volumeSourceFactMissingProducesNoDerivedResult() {
        ProcessEffortSourceKnowledge bothFacts = sourceKnowledge("4", "3", "item-1", "item-1");
        ProcessEffortSourceKnowledge sourceKnowledge = new ProcessEffortSourceKnowledge(
                bothFacts.evidenceBase(),
                List.of(sourceFact(bothFacts, ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID)));

        assertThat(materializer.materialize(sourceKnowledge)).isEmpty();
    }

    @Test
    void effortSourceFactMissingProducesNoDerivedResult() {
        ProcessEffortSourceKnowledge bothFacts = sourceKnowledge("4", "3", "item-1", "item-1");
        ProcessEffortSourceKnowledge sourceKnowledge = new ProcessEffortSourceKnowledge(
                bothFacts.evidenceBase(),
                List.of(sourceFact(bothFacts, ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID)));

        assertThat(materializer.materialize(sourceKnowledge)).isEmpty();
    }

    @Test
    void projectionPresentButSourceFactMissingDueLabelRejectionProducesNoDerivedResult() {
        ProcessAnalysisResult blankVolumeLabel = map(evidence(
                volume("4", "item-1", " "),
                effort("3", "item-1", "request")));
        ProcessAnalysisResult blankEffortLabel = map(evidence(
                volume("4", "item-1", "request"),
                effort("3", "item-1", " ")));

        assertThat(blankVolumeLabel.volumeProjection()).isPresent();
        assertThat(blankVolumeLabel.effortProjection()).isPresent();
        assertThat(blankVolumeLabel.sourceKnowledge().knownFacts())
                .extracting(ProcessKnownFact::id)
                .containsExactly(ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID);
        assertThat(materializer.materialize(blankVolumeLabel.sourceKnowledge())).isEmpty();
        assertThat(blankEffortLabel.volumeProjection()).isPresent();
        assertThat(blankEffortLabel.effortProjection()).isPresent();
        assertThat(blankEffortLabel.sourceKnowledge().knownFacts())
                .extracting(ProcessKnownFact::id)
                .containsExactly(ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID);
        assertThat(materializer.materialize(blankEffortLabel.sourceKnowledge())).isEmpty();
    }

    @Test
    void missingOperandProjectionProducesNoDerivedResult() {
        ProcessEffortSourceKnowledge bothFacts = sourceKnowledge("4", "3", "item-1", "item-1");
        ProcessKnownFact volumeWithoutProjection = sourceFactWithoutProjection(
                ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID);
        ProcessEffortSourceKnowledge sourceKnowledge = new ProcessEffortSourceKnowledge(
                bothFacts.evidenceBase(),
                List.of(volumeWithoutProjection, sourceFact(bothFacts, ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID)));

        assertThat(materializer.materialize(sourceKnowledge)).isEmpty();
    }

    @Test
    void wrongUnitRoleProducesNoDerivedResult() {
        ProcessEffortSourceKnowledge bothFacts = sourceKnowledge("4", "3", "item-1", "item-1");
        ProcessKnownFact wrongVolume = sourceQuantityFact(
                ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
                "4",
                new ProcessEffortPerBusinessItemUnit(
                        ProcessEffortDurationUnit.MINUTE,
                        new ProcessBusinessItemUnitId("item-1")));
        ProcessEffortSourceKnowledge sourceKnowledge = new ProcessEffortSourceKnowledge(
                bothFacts.evidenceBase(),
                List.of(wrongVolume, sourceFact(bothFacts, ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID)));

        assertThat(materializer.materialize(sourceKnowledge)).isEmpty();
    }

    @Test
    void differentBusinessItemIdsProduceNoDerivedResult() {
        assertThat(materializer.materialize(sourceKnowledge("4", "3", "item-1", "item-2"))).isEmpty();
    }

    @Test
    void nonProcessStatusesProduceNoDerivedResult() {
        for (ProcessAnalysisStatus status : List.of(
                ProcessAnalysisStatus.OUT_OF_SCOPE,
                ProcessAnalysisStatus.INSUFFICIENT_INFORMATION)) {
            ProcessAnalysisModelResult modelResult = new ProcessAnalysisModelResult(
                    understanding(status),
                    evidence(volume("4", "item-1", "request"), effort("3", "item-1", "request")));
            ProcessAnalysisResult mapped = sourceMapper.map(modelResult);

            assertThat(mapped.sourceKnowledge().knownFacts()).isEmpty();
            assertThat(materializer.materialize(mapped.sourceKnowledge())).isEmpty();
        }
    }

    @Test
    void nonExactEvidenceThatPreventsSourceFactMaterializationProducesNoDerivedResult() {
        ProcessAnalysisResult mapped = map(evidence(
                quantity(
                        ProcessEffortEvidenceQuantityStatus.RANGE,
                        null,
                        "item-1",
                        "request",
                        ProcessReportingPeriodUnit.MONTH,
                        null),
                quantity(
                        ProcessEffortEvidenceQuantityStatus.APPROXIMATE,
                        "3",
                        "item-1",
                        "request",
                        null,
                        ProcessEffortDurationUnit.MINUTE)));

        assertThat(mapped.sourceKnowledge().knownFacts()).isEmpty();
        assertThat(materializer.materialize(mapped.sourceKnowledge())).isEmpty();
    }

    @Test
    void productionRuntimeTypesDoNotDependOnDerivationVerifier() {
        assertThat(declaredFieldTypes(ProcessAnalysisApplicationService.class))
                .doesNotContain(ProcessEffortPerReportingPeriodDerivationVerifier.class);
        assertThat(constructorParameterTypes(ProcessAnalysisApplicationService.class))
                .doesNotContain(ProcessEffortPerReportingPeriodDerivationVerifier.class);
        assertThat(declaredFieldTypes(ProcessEffortPerReportingPeriodMaterializer.class))
                .doesNotContain(ProcessEffortPerReportingPeriodDerivationVerifier.class);
        assertThat(constructorParameterTypes(ProcessEffortPerReportingPeriodMaterializer.class))
                .doesNotContain(ProcessEffortPerReportingPeriodDerivationVerifier.class);
        assertThat(declaredFieldTypes(ProcessEffortEvidenceProjectionMapper.class))
                .doesNotContain(ProcessEffortPerReportingPeriodDerivationVerifier.class);
        assertThat(constructorParameterTypes(ProcessEffortEvidenceProjectionMapper.class))
                .doesNotContain(ProcessEffortPerReportingPeriodDerivationVerifier.class);
    }

    private ProcessEffortDerivedResult materialize(ProcessEffortSourceKnowledge sourceKnowledge) {
        return materializer.materialize(sourceKnowledge).orElseThrow();
    }

    private ProcessEffortSourceKnowledge sourceKnowledge(
            String volumeMagnitude,
            String effortMagnitude,
            String volumeRef,
            String effortRef) {
        return map(evidence(
                volume(volumeMagnitude, volumeRef, "request"),
                effort(effortMagnitude, effortRef, "request"))).sourceKnowledge();
    }

    private ProcessAnalysisResult map(ProcessEffortEvidence evidence) {
        return sourceMapper.map(new ProcessAnalysisModelResult(
                understanding(ProcessAnalysisStatus.PROCESS_IDENTIFIED),
                evidence));
    }

    private ProcessKnownFact sourceFact(ProcessEffortSourceKnowledge sourceKnowledge, ProcessKnownFactId factId) {
        return sourceKnowledge.knownFacts().stream()
                .filter(fact -> fact.id().equals(factId))
                .findFirst()
                .orElseThrow();
    }

    private ProcessKnownFact sourceQuantityFact(
            ProcessKnownFactId factId,
            String magnitude,
            com.codeworkdigital.api.processanalysis.domain.ProcessQuantityUnitExpression unit) {
        return new ProcessKnownFact(
                factId,
                "source statement",
                ProcessFactGrounding.SOURCE_STATED,
                ProcessAnalysisScope.processWide(),
                List.of(),
                List.of(ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID),
                Optional.of(new ProcessQuantityProjection(new BigDecimal(magnitude), unit)));
    }

    private ProcessKnownFact sourceFactWithoutProjection(ProcessKnownFactId factId) {
        return new ProcessKnownFact(
                factId,
                "source statement",
                ProcessFactGrounding.SOURCE_STATED,
                ProcessAnalysisScope.processWide(),
                List.of(),
                List.of(ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID),
                Optional.empty());
    }

    private ProcessQuantityProjection quantityProjection(ProcessEffortDerivedResult result) {
        return (ProcessQuantityProjection) result.resultFact().computableProjection().orElseThrow();
    }

    private ProcessEffortEvidence evidence(
            ProcessEffortEvidenceQuantity volume,
            ProcessEffortEvidenceQuantity effort) {
        return new ProcessEffortEvidence(volume, effort);
    }

    private ProcessEffortEvidenceQuantity volume(
            String magnitude,
            String businessItemRef,
            String businessItemLabel) {
        return quantity(
                ProcessEffortEvidenceQuantityStatus.EXACT,
                magnitude,
                businessItemRef,
                businessItemLabel,
                ProcessReportingPeriodUnit.MONTH,
                null);
    }

    private ProcessEffortEvidenceQuantity effort(
            String magnitude,
            String businessItemRef,
            String businessItemLabel) {
        return quantity(
                ProcessEffortEvidenceQuantityStatus.EXACT,
                magnitude,
                businessItemRef,
                businessItemLabel,
                null,
                ProcessEffortDurationUnit.MINUTE);
    }

    private ProcessEffortEvidenceQuantity quantity(
            ProcessEffortEvidenceQuantityStatus status,
            String magnitude,
            String businessItemRef,
            String businessItemLabel,
            ProcessReportingPeriodUnit reportingPeriod,
            ProcessEffortDurationUnit effortDuration) {
        return new ProcessEffortEvidenceQuantity(
                status,
                magnitude == null ? null : new BigDecimal(magnitude),
                null,
                null,
                businessItemRef,
                businessItemLabel,
                reportingPeriod,
                effortDuration,
                null,
                null);
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

    private ProcessUnderstanding understanding(ProcessAnalysisStatus status) {
        if (status != ProcessAnalysisStatus.PROCESS_IDENTIFIED) {
            return new ProcessUnderstanding(
                    "Description",
                    status,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    "");
        }
        return new ProcessUnderstanding(
                "Description",
                List.of("A request is received."),
                List.of("A manual check may occur."),
                List.of(),
                List.of(new ProcessUnderstandingStage(
                        "receive-request",
                        "Receive request",
                        "The team receives a request.",
                        ProcessStageProvenance.OBSERVED,
                        ProcessStageOperationType.RECEIVE,
                        ProcessStageInputNature.UNSTRUCTURED)),
                "This understanding is preliminary.");
    }
}
