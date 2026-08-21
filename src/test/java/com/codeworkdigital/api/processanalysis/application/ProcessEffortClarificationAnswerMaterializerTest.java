package com.codeworkdigital.api.processanalysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeworkdigital.api.processanalysis.domain.ProcessAnalysisScope;
import com.codeworkdigital.api.processanalysis.domain.ProcessBusinessItemPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerBusinessItemUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceGap;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceSource;
import com.codeworkdigital.api.processanalysis.domain.ProcessFactGrounding;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFact;
import com.codeworkdigital.api.processanalysis.domain.ProcessQuantityProjection;
import com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessEffortClarificationAnswerMaterializerTest {

    private final ProcessEffortClarificationAnswerMaterializer materializer =
            new ProcessEffortClarificationAnswerMaterializer();

    @Test
    void zeroAnswerIsValid() {
        ProcessEffortMaterialityClarificationAnswer answer = answer(
                ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD,
                "0");

        ProcessEffortClarificationKnowledge knowledge = materializer.materialize(
                gaps(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD),
                evidence(absent("item-from-evidence", "ticket"), exactEffort("2", "item-from-evidence", "ticket")),
                List.of(answer));

        assertThat(volumeProjection(knowledge).magnitude()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void decimalAnswerIsValid() {
        ProcessEffortClarificationKnowledge knowledge = materializer.materialize(
                gaps(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM),
                evidence(exactVolume("4000", "item-from-evidence", "ticket"), absent("item-from-evidence", "ticket")),
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM, "2.5")));

        assertThat(effortProjection(knowledge).magnitude()).isEqualByComparingTo("2.5");
    }

    @Test
    void negativeAnswerIsRejected() {
        assertThatThrownBy(() -> answer(
                ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD,
                "-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("magnitude");
    }

    @Test
    void volumeAnswerForActionableVolumeGapMaterializesCanonicalSourceStatedFact() {
        ProcessEffortClarificationKnowledge knowledge = materializer.materialize(
                gaps(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD),
                evidence(absent("item-from-evidence", "ticket"), exactEffort("2", "item-from-evidence", "ticket")),
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000")));

        assertThat(knowledge.knownFacts()).singleElement().satisfies(fact -> {
            assertThat(fact.id()).isEqualTo(ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID);
            assertThat(fact.grounding()).isEqualTo(ProcessFactGrounding.SOURCE_STATED);
            assertThat(fact.scope().isProcessWide()).isTrue();
            assertThat(fact.premiseFactIds()).isEmpty();
            assertThat(fact.evidenceArtifactIds())
                    .containsExactly(ProcessEffortClarificationAnswerMaterializer.VOLUME_CLARIFICATION_ARTIFACT_ID);
            assertThat(fact.evidenceArtifactIds())
                    .doesNotContain(ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID);
            assertThat(fact.statement())
                    .isEqualTo("Stated volume for business item 'ticket': 4000 per month");
        });
    }

    @Test
    void effortAnswerForActionableEffortGapMaterializesCanonicalSourceStatedFact() {
        ProcessEffortClarificationKnowledge knowledge = materializer.materialize(
                gaps(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM),
                evidence(exactVolume("4000", "item-from-evidence", "ticket"), absent("item-from-evidence", "ticket")),
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM, "2")));

        assertThat(knowledge.knownFacts()).singleElement().satisfies(fact -> {
            assertThat(fact.id()).isEqualTo(ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID);
            assertThat(fact.grounding()).isEqualTo(ProcessFactGrounding.SOURCE_STATED);
            assertThat(fact.scope().isProcessWide()).isTrue();
            assertThat(fact.premiseFactIds()).isEmpty();
            assertThat(fact.evidenceArtifactIds())
                    .containsExactly(ProcessEffortClarificationAnswerMaterializer.EFFORT_CLARIFICATION_ARTIFACT_ID);
            assertThat(fact.evidenceArtifactIds())
                    .doesNotContain(ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID);
            assertThat(fact.statement())
                    .isEqualTo("Stated effort for business item 'ticket': 2 minute per item");
        });
    }

    @Test
    void volumeFactUsesMonthAndExistingBusinessItemRef() {
        ProcessQuantityProjection projection = volumeProjection(materializer.materialize(
                gaps(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD),
                evidence(absent("source-item-ref", "ticket"), exactEffort("2", "source-item-ref", "ticket")),
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000"))));

        assertThat(projection.unit()).isInstanceOfSatisfying(
                ProcessBusinessItemPerReportingPeriodUnit.class,
                unit -> {
                    assertThat(unit.businessItemId().value()).isEqualTo("source-item-ref");
                    assertThat(unit.reportingPeriod()).isEqualTo(ProcessReportingPeriodUnit.MONTH);
                });
    }

    @Test
    void effortFactUsesMinuteAndExistingBusinessItemRef() {
        ProcessQuantityProjection projection = effortProjection(materializer.materialize(
                gaps(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM),
                evidence(exactVolume("4000", "source-item-ref", "ticket"), absent("source-item-ref", "ticket")),
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM, "2"))));

        assertThat(projection.unit()).isInstanceOfSatisfying(
                ProcessEffortPerBusinessItemUnit.class,
                unit -> {
                    assertThat(unit.effortDuration()).isEqualTo(ProcessEffortDurationUnit.MINUTE);
                    assertThat(unit.businessItemId().value()).isEqualTo("source-item-ref");
                });
    }

    @Test
    void businessItemLabelComesFromExistingEvidenceContextAndIsNotCanonicalized() {
        ProcessEffortClarificationKnowledge knowledge = materializer.materialize(
                gaps(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD),
                evidence(absent("item-1", "  Ticket Label  "), exactEffort("2", "item-1", "ignored")),
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000")));

        assertThat(knowledge.knownFacts().getFirst().statement())
                .isEqualTo("Stated volume for business item '  Ticket Label  ': 4000 per month");
    }

    @Test
    void statementIsDeterministicFromProjectionAndExistingLabel() {
        ProcessEffortClarificationKnowledge first = materializer.materialize(
                gaps(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM),
                evidence(exactVolume("4000", "item-1", "ticket"), absentWithEvidenceText("item-1", "ticket", "999 hours")),
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM, "2.00")));
        ProcessEffortClarificationKnowledge second = materializer.materialize(
                gaps(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM),
                evidence(exactVolume("4000", "item-1", "ticket"), absentWithEvidenceText("item-1", "ticket", "123 days")),
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM, "2.00")));

        assertThat(first.knownFacts().getFirst().computableProjection())
                .isEqualTo(second.knownFacts().getFirst().computableProjection());
        assertThat(first.knownFacts().getFirst().statement())
                .isEqualTo(second.knownFacts().getFirst().statement())
                .isEqualTo("Stated effort for business item 'ticket': 2 minute per item");
        assertThat(first.knownFacts().getFirst().statement()).doesNotContain("999", "hours", "123", "days");
    }

    @Test
    void clarificationArtifactsAreDedicatedSourceMaterialWithExplicitDescriptions() {
        ProcessEffortClarificationKnowledge knowledge = materializer.materialize(
                gaps(
                        ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD,
                        ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM),
                evidence(absent("item-1", "ticket"), absent("item-1", "ticket")),
                List.of(
                        answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000"),
                        answer(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM, "2")));

        assertThat(knowledge.evidenceBase().artifacts()).hasSize(2);
        assertThat(knowledge.evidenceBase().artifacts().get(0)).satisfies(artifact -> {
            assertThat(artifact.id())
                    .isEqualTo(ProcessEffortClarificationAnswerMaterializer.VOLUME_CLARIFICATION_ARTIFACT_ID);
            assertThat(artifact.kind().name()).isEqualTo("SOURCE_MATERIAL");
            assertThat(artifact.description())
                    .isEqualTo("Self-reported clarification answer for P06 volume per reporting period");
        });
        assertThat(knowledge.evidenceBase().artifacts().get(1)).satisfies(artifact -> {
            assertThat(artifact.id())
                    .isEqualTo(ProcessEffortClarificationAnswerMaterializer.EFFORT_CLARIFICATION_ARTIFACT_ID);
            assertThat(artifact.kind().name()).isEqualTo("SOURCE_MATERIAL");
            assertThat(artifact.description())
                    .isEqualTo("Self-reported clarification answer for P06 effort per business item");
        });
    }

    @Test
    void answerWithoutCurrentlyActionableGapCannotMaterializeFact() {
        assertThatThrownBy(() -> materializer.materialize(
                gaps(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM),
                evidence(absent("item-1", "ticket"), absent("item-1", "ticket")),
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not currently actionable");
    }

    @Test
    void duplicateAnswersForOneKindAreRejected() {
        assertThatThrownBy(() -> materializer.materialize(
                gaps(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD),
                evidence(absent("item-1", "ticket"), exactEffort("2", "item-1", "ticket")),
                List.of(
                        answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000"),
                        answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "5000"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate clarification answer kind");
    }

    @Test
    void bothValidAnswersProduceDeterministicVolumeThenEffortOrdering() {
        ProcessEffortClarificationKnowledge knowledge = materializer.materialize(
                gaps(
                        ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM,
                        ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD),
                evidence(absent("item-1", "ticket"), absent("item-1", "ticket")),
                List.of(
                        answer(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM, "2"),
                        answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000")));

        assertThat(knowledge.evidenceBase().artifacts())
                .extracting(artifact -> artifact.id().value())
                .containsExactly(
                        "source-clarification-volume-per-reporting-period",
                        "source-clarification-effort-per-business-item");
        assertThat(knowledge.knownFacts())
                .extracting(ProcessKnownFact::id)
                .containsExactly(
                        ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
                        ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID);
    }

    @Test
    void materializerDoesNotMutateOriginalEvidenceOrSourceKnowledgeAndIsNotWiredIntoProductionOrchestration() {
        ProcessEffortEvidence evidence = evidence(
                absent("item-1", "ticket"),
                exactEffort("2", "item-1", "ticket"));
        ProcessEffortSourceKnowledge sourceKnowledge = new ProcessEffortEvidenceProjectionMapper()
                .map(new ProcessAnalysisModelResult(understanding(), evidence))
                .sourceKnowledge();

        ProcessEffortClarificationKnowledge clarificationKnowledge = materializer.materialize(
                gaps(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD),
                evidence,
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000")));

        assertThat(evidence.volumePerReportingPeriod().status())
                .isEqualTo(ProcessEffortEvidenceQuantityStatus.ABSENT);
        assertThat(sourceKnowledge.knownFacts())
                .singleElement()
                .satisfies(fact -> assertThat(fact.evidenceArtifactIds())
                        .containsExactly(ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID));
        assertThat(clarificationKnowledge.knownFacts())
                .singleElement()
                .satisfies(fact -> assertThat(fact.evidenceArtifactIds())
                        .containsExactly(ProcessEffortClarificationAnswerMaterializer.VOLUME_CLARIFICATION_ARTIFACT_ID));
        assertThat(declaredFieldTypes(ProcessAnalysisApplicationService.class))
                .doesNotContain(ProcessEffortClarificationAnswerMaterializer.class);
        assertThat(constructorParameterTypes(ProcessAnalysisApplicationService.class))
                .doesNotContain(ProcessEffortClarificationAnswerMaterializer.class);
    }

    @Test
    void noModelInvocationOrVerifierDependencyIsIntroduced() {
        assertThat(declaredFieldTypes(ProcessEffortClarificationAnswerMaterializer.class))
                .doesNotContain(ProcessAnalysisModelClient.class, ProcessEffortPerReportingPeriodDerivationVerifier.class);
        assertThat(constructorParameterTypes(ProcessEffortClarificationAnswerMaterializer.class))
                .doesNotContain(ProcessAnalysisModelClient.class, ProcessEffortPerReportingPeriodDerivationVerifier.class);
    }

    private ProcessQuantityProjection volumeProjection(ProcessEffortClarificationKnowledge knowledge) {
        return (ProcessQuantityProjection) knowledge.knownFacts().stream()
                .filter(fact -> fact.id().equals(ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID))
                .findFirst()
                .orElseThrow()
                .computableProjection()
                .orElseThrow();
    }

    private ProcessQuantityProjection effortProjection(ProcessEffortClarificationKnowledge knowledge) {
        return (ProcessQuantityProjection) knowledge.knownFacts().stream()
                .filter(fact -> fact.id().equals(ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID))
                .findFirst()
                .orElseThrow()
                .computableProjection()
                .orElseThrow();
    }

    private ProcessEffortMaterialityClarificationAnswer answer(
            ProcessEffortMaterialityEvidenceGapKind kind,
            String magnitude) {
        return new ProcessEffortMaterialityClarificationAnswer(kind, new BigDecimal(magnitude));
    }

    private List<ProcessEffortMaterialityEvidenceGap> gaps(ProcessEffortMaterialityEvidenceGapKind... kinds) {
        return Arrays.stream(kinds)
                .map(kind -> new ProcessEffortMaterialityEvidenceGap(
                        kind,
                        new ProcessEvidenceGap(
                                "question",
                                ProcessEvidenceSource.SELF_REPORTED,
                                ProcessEffortMaterialityEvidenceGapIdentifier.DECISION_AFFECTED,
                                ProcessAnalysisScope.processWide())))
                .toList();
    }

    private ProcessEffortEvidence evidence(
            ProcessEffortEvidenceQuantity volume,
            ProcessEffortEvidenceQuantity effort) {
        return new ProcessEffortEvidence(volume, effort);
    }

    private ProcessEffortEvidenceQuantity absent(String businessItemRef, String businessItemLabel) {
        return new ProcessEffortEvidenceQuantity(
                ProcessEffortEvidenceQuantityStatus.ABSENT,
                null,
                null,
                null,
                businessItemRef,
                businessItemLabel,
                null,
                null,
                null,
                null);
    }

    private ProcessEffortEvidenceQuantity absentWithEvidenceText(
            String businessItemRef,
            String businessItemLabel,
            String evidenceText) {
        return new ProcessEffortEvidenceQuantity(
                ProcessEffortEvidenceQuantityStatus.ABSENT,
                null,
                null,
                null,
                businessItemRef,
                businessItemLabel,
                null,
                null,
                evidenceText,
                null);
    }

    private ProcessEffortEvidenceQuantity exactVolume(
            String magnitude,
            String businessItemRef,
            String businessItemLabel) {
        return new ProcessEffortEvidenceQuantity(
                ProcessEffortEvidenceQuantityStatus.EXACT,
                new BigDecimal(magnitude),
                null,
                null,
                businessItemRef,
                businessItemLabel,
                ProcessReportingPeriodUnit.MONTH,
                null,
                null,
                null);
    }

    private ProcessEffortEvidenceQuantity exactEffort(
            String magnitude,
            String businessItemRef,
            String businessItemLabel) {
        return new ProcessEffortEvidenceQuantity(
                ProcessEffortEvidenceQuantityStatus.EXACT,
                new BigDecimal(magnitude),
                null,
                null,
                businessItemRef,
                businessItemLabel,
                null,
                ProcessEffortDurationUnit.MINUTE,
                null,
                null);
    }

    private ProcessUnderstanding understanding() {
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
}
