package com.codeworkdigital.api.processanalysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeworkdigital.api.processanalysis.domain.ProcessBusinessItemPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerBusinessItemUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceArtifactKind;
import com.codeworkdigital.api.processanalysis.domain.ProcessFactGrounding;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFact;
import com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessEffortEvidenceProjectionMapperTest {

    private final ProcessEffortEvidenceProjectionMapper mapper = new ProcessEffortEvidenceProjectionMapper();

    @Test
    void mapsExactMonthlyVolumeToQuantityProjection() {
        ProcessAnalysisResult result = map(evidence(
                volume(ProcessEffortEvidenceQuantityStatus.EXACT, "4000", "item-1", "request"),
                effort(ProcessEffortEvidenceQuantityStatus.ABSENT, null, null, null)));

        assertThat(result.volumeProjection()).isPresent();
        assertThat(result.volumeProjection().get().magnitude()).isEqualByComparingTo("4000");
        assertThat(result.volumeProjection().get().unit()).isInstanceOfSatisfying(
                ProcessBusinessItemPerReportingPeriodUnit.class,
                unit -> {
                    assertThat(unit.businessItemId().value()).isEqualTo("item-1");
                    assertThat(unit.reportingPeriod()).isEqualTo(ProcessReportingPeriodUnit.MONTH);
                });
        assertThat(result.effortProjection()).isEmpty();
        assertThat(result.composable()).isFalse();
        assertThat(result.sourceKnowledge().knownFacts())
                .singleElement()
                .satisfies(fact -> {
                    assertThat(fact.id()).isEqualTo(ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID);
                    assertThat(fact.grounding()).isEqualTo(ProcessFactGrounding.SOURCE_STATED);
                    assertThat(fact.scope().isProcessWide()).isTrue();
                    assertThat(fact.premiseFactIds()).isEmpty();
                    assertThat(fact.evidenceArtifactIds())
                            .containsExactly(ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID);
                    assertThat(fact.computableProjection()).contains(result.volumeProjection().orElseThrow());
                    assertThat(fact.statement()).isEqualTo("4000 business items per month are stated for this process");
                });
    }

    @Test
    void mapsExactMinuteEffortToQuantityProjection() {
        ProcessAnalysisResult result = map(evidence(
                volume(ProcessEffortEvidenceQuantityStatus.ABSENT, null, null, null),
                effort(ProcessEffortEvidenceQuantityStatus.EXACT, "2", "item-1", "request")));

        assertThat(result.volumeProjection()).isEmpty();
        assertThat(result.effortProjection()).isPresent();
        assertThat(result.effortProjection().get().magnitude()).isEqualByComparingTo("2");
        assertThat(result.effortProjection().get().unit()).isInstanceOfSatisfying(
                ProcessEffortPerBusinessItemUnit.class,
                unit -> {
                    assertThat(unit.effortDuration()).isEqualTo(ProcessEffortDurationUnit.MINUTE);
                    assertThat(unit.businessItemId().value()).isEqualTo("item-1");
                });
        assertThat(result.composable()).isFalse();
        assertThat(result.sourceKnowledge().knownFacts())
                .singleElement()
                .satisfies(fact -> {
                    assertThat(fact.id()).isEqualTo(ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID);
                    assertThat(fact.grounding()).isEqualTo(ProcessFactGrounding.SOURCE_STATED);
                    assertThat(fact.scope().isProcessWide()).isTrue();
                    assertThat(fact.premiseFactIds()).isEmpty();
                    assertThat(fact.evidenceArtifactIds())
                            .containsExactly(ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID);
                    assertThat(fact.computableProjection()).contains(result.effortProjection().orElseThrow());
                    assertThat(fact.statement()).isEqualTo("2 minute per business item is stated for this process");
                });
    }

    @Test
    void eligibleVolumeAndEffortProduceTwoSourceStatedAtomicFactsOnOneSourceArtifact() {
        ProcessAnalysisResult result = map(evidence(
                volume(ProcessEffortEvidenceQuantityStatus.EXACT, "4000", "item-1", "request"),
                effort(ProcessEffortEvidenceQuantityStatus.EXACT, "2", "item-1", "request")));

        ProcessEffortSourceKnowledge sourceKnowledge = result.sourceKnowledge();

        assertThat(sourceKnowledge.evidenceBase().artifacts())
                .singleElement()
                .satisfies(artifact -> {
                    assertThat(artifact.id()).isEqualTo(ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID);
                    assertThat(artifact.kind()).isEqualTo(ProcessEvidenceArtifactKind.SOURCE_MATERIAL);
                    assertThat(artifact.description())
                            .isEqualTo("Process description submitted for this analysis");
                });
        assertThat(sourceKnowledge.knownFacts()).hasSize(2);
        assertThat(sourceKnowledge.knownFacts())
                .extracting(ProcessKnownFact::id)
                .containsExactly(
                        ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
                        ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID);
        assertThat(sourceKnowledge.knownFacts())
                .allSatisfy(fact -> {
                    assertThat(fact.grounding()).isEqualTo(ProcessFactGrounding.SOURCE_STATED);
                    assertThat(fact.scope().isProcessWide()).isTrue();
                    assertThat(fact.premiseFactIds()).isEmpty();
                    assertThat(fact.evidenceArtifactIds())
                            .containsExactly(ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID);
                });
        assertThat(sourceKnowledge.knownFacts().get(0).computableProjection())
                .contains(result.volumeProjection().orElseThrow());
        assertThat(sourceKnowledge.knownFacts().get(1).computableProjection())
                .contains(result.effortProjection().orElseThrow());
    }

    @Test
    void sourceFactStatementsAreDeterministicAndRenderedFromAdmittedProjections() {
        ProcessAnalysisResult first = map(evidence(
                volume(ProcessEffortEvidenceQuantityStatus.EXACT, "4000.00", "item-1", "request"),
                new ProcessEffortEvidenceQuantity(
                        ProcessEffortEvidenceQuantityStatus.EXACT,
                        new BigDecimal("2.00"),
                        null,
                        null,
                        "item-1",
                        "ticket",
                        null,
                        ProcessEffortDurationUnit.MINUTE,
                        "a contradictory provider phrase that says 999 hours",
                        null)));
        ProcessAnalysisResult second = map(evidence(
                volume(ProcessEffortEvidenceQuantityStatus.EXACT, "4000.00", "item-1", "changed label"),
                new ProcessEffortEvidenceQuantity(
                        ProcessEffortEvidenceQuantityStatus.EXACT,
                        new BigDecimal("2.00"),
                        null,
                        null,
                        "item-1",
                        "changed label",
                        null,
                        ProcessEffortDurationUnit.MINUTE,
                        "different raw evidence text with 123 days",
                        null)));

        assertThat(second.volumeProjection()).isEqualTo(first.volumeProjection());
        assertThat(second.effortProjection()).isEqualTo(first.effortProjection());
        assertThat(second.sourceKnowledge().knownFacts()).isEqualTo(first.sourceKnowledge().knownFacts());
        assertThat(first.sourceKnowledge().knownFacts())
                .extracting(ProcessKnownFact::statement)
                .containsExactly(
                        "4000 business items per month are stated for this process",
                        "2 minute per business item is stated for this process");
    }

    @Test
    void zeroExactVolumeIsValid() {
        ProcessAnalysisResult result = map(evidence(
                volume(ProcessEffortEvidenceQuantityStatus.EXACT, "0", "item-1", "request"),
                effort(ProcessEffortEvidenceQuantityStatus.ABSENT, null, null, null)));

        assertThat(result.volumeProjection()).isPresent();
        assertThat(result.volumeProjection().get().magnitude()).isEqualByComparingTo("0");
        assertThat(result.sourceKnowledge().knownFacts())
                .singleElement()
                .satisfies(fact -> assertThat(fact.computableProjection())
                        .contains(result.volumeProjection().orElseThrow()));
    }

    @Test
    void zeroExactEffortIsValid() {
        ProcessAnalysisResult result = map(evidence(
                volume(ProcessEffortEvidenceQuantityStatus.ABSENT, null, null, null),
                effort(ProcessEffortEvidenceQuantityStatus.EXACT, "0", "item-1", "request")));

        assertThat(result.effortProjection()).isPresent();
        assertThat(result.effortProjection().get().magnitude()).isEqualByComparingTo("0");
        assertThat(result.sourceKnowledge().knownFacts())
                .singleElement()
                .satisfies(fact -> assertThat(fact.computableProjection())
                        .contains(result.effortProjection().orElseThrow()));
    }

    @Test
    void negativeExactVolumeDoesNotPromote() {
        ProcessAnalysisResult result = map(evidence(
                volume(ProcessEffortEvidenceQuantityStatus.EXACT, "-1", "item-1", "request"),
                effort(ProcessEffortEvidenceQuantityStatus.EXACT, "2", "item-1", "request")));

        assertThat(result.volumeProjection()).isEmpty();
        assertThat(result.effortProjection()).isPresent();
        assertThat(result.composable()).isFalse();
        assertThat(result.sourceKnowledge().knownFacts())
                .extracting(ProcessKnownFact::id)
                .containsExactly(ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID);
    }

    @Test
    void negativeExactEffortDoesNotPromote() {
        ProcessAnalysisResult result = map(evidence(
                volume(ProcessEffortEvidenceQuantityStatus.EXACT, "4000", "item-1", "request"),
                effort(ProcessEffortEvidenceQuantityStatus.EXACT, "-1", "item-1", "request")));

        assertThat(result.volumeProjection()).isPresent();
        assertThat(result.effortProjection()).isEmpty();
        assertThat(result.composable()).isFalse();
        assertThat(result.sourceKnowledge().knownFacts())
                .extracting(ProcessKnownFact::id)
                .containsExactly(ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID);
    }

    @Test
    void exactVolumeWithMinMagnitudeDoesNotPromote() {
        ProcessAnalysisResult result = map(evidence(
                volumeWithRangeFields("4000", "3000", null),
                effort(ProcessEffortEvidenceQuantityStatus.EXACT, "2", "item-1", "request")));

        assertThat(result.volumeProjection()).isEmpty();
        assertThat(result.effortProjection()).isPresent();
        assertThat(result.composable()).isFalse();
    }

    @Test
    void exactVolumeWithMaxMagnitudeDoesNotPromote() {
        ProcessAnalysisResult result = map(evidence(
                volumeWithRangeFields("4000", null, "5000"),
                effort(ProcessEffortEvidenceQuantityStatus.EXACT, "2", "item-1", "request")));

        assertThat(result.volumeProjection()).isEmpty();
        assertThat(result.effortProjection()).isPresent();
        assertThat(result.composable()).isFalse();
    }

    @Test
    void exactEffortWithMinMagnitudeDoesNotPromote() {
        ProcessAnalysisResult result = map(evidence(
                volume(ProcessEffortEvidenceQuantityStatus.EXACT, "4000", "item-1", "request"),
                effortWithRangeFields("2", "1", null)));

        assertThat(result.volumeProjection()).isPresent();
        assertThat(result.effortProjection()).isEmpty();
        assertThat(result.composable()).isFalse();
        assertThat(result.sourceKnowledge().knownFacts())
                .extracting(ProcessKnownFact::id)
                .containsExactly(ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID);
    }

    @Test
    void exactEffortWithMaxMagnitudeDoesNotPromote() {
        ProcessAnalysisResult result = map(evidence(
                volume(ProcessEffortEvidenceQuantityStatus.EXACT, "4000", "item-1", "request"),
                effortWithRangeFields("2", null, "3")));

        assertThat(result.volumeProjection()).isPresent();
        assertThat(result.effortProjection()).isEmpty();
        assertThat(result.composable()).isFalse();
    }

    @Test
    void exactVolumeWithEffortDurationDoesNotPromote() {
        ProcessAnalysisResult result = map(evidence(
                new ProcessEffortEvidenceQuantity(
                        ProcessEffortEvidenceQuantityStatus.EXACT,
                        new BigDecimal("4000"),
                        null,
                        null,
                        "item-1",
                        "request",
                        ProcessReportingPeriodUnit.MONTH,
                        ProcessEffortDurationUnit.MINUTE,
                        "4000 requests per month",
                        null),
                effort(ProcessEffortEvidenceQuantityStatus.EXACT, "2", "item-1", "request")));

        assertThat(result.volumeProjection()).isEmpty();
        assertThat(result.effortProjection()).isPresent();
        assertThat(result.composable()).isFalse();
    }

    @Test
    void exactEffortWithReportingPeriodDoesNotPromote() {
        ProcessAnalysisResult result = map(evidence(
                volume(ProcessEffortEvidenceQuantityStatus.EXACT, "4000", "item-1", "request"),
                new ProcessEffortEvidenceQuantity(
                        ProcessEffortEvidenceQuantityStatus.EXACT,
                        new BigDecimal("2"),
                        null,
                        null,
                        "item-1",
                        "request",
                        ProcessReportingPeriodUnit.MONTH,
                        ProcessEffortDurationUnit.MINUTE,
                        "2 minutes per request",
                        null)));

        assertThat(result.volumeProjection()).isPresent();
        assertThat(result.effortProjection()).isEmpty();
        assertThat(result.composable()).isFalse();
    }

    @Test
    void equalLocalRefsOnExactSupportedProjectionsAreComposable() {
        ProcessAnalysisResult result = map(evidence(
                volume(ProcessEffortEvidenceQuantityStatus.EXACT, "4000", "item-1", "request"),
                effort(ProcessEffortEvidenceQuantityStatus.EXACT, "2", "item-1", "ticket")));

        assertThat(result.volumeProjection()).isPresent();
        assertThat(result.effortProjection()).isPresent();
        assertThat(result.composable()).isTrue();
        assertThat(result.sourceKnowledge().knownFacts()).hasSize(2);
    }

    @Test
    void differentLocalRefsAreNotComposableEvenWhenLabelsMatch() {
        ProcessAnalysisResult result = map(evidence(
                volume(ProcessEffortEvidenceQuantityStatus.EXACT, "4000", "item-1", "request"),
                effort(ProcessEffortEvidenceQuantityStatus.EXACT, "2", "item-2", "request")));

        assertThat(result.volumeProjection()).isPresent();
        assertThat(result.effortProjection()).isPresent();
        assertThat(result.composable()).isFalse();
    }

    @Test
    void rangeNeverBecomesProductionProjection() {
        ProcessAnalysisResult result = map(evidence(
                new ProcessEffortEvidenceQuantity(
                        ProcessEffortEvidenceQuantityStatus.RANGE,
                        null,
                        new BigDecimal("4"),
                        new BigDecimal("5"),
                        "item-1",
                        "request",
                        ProcessReportingPeriodUnit.MONTH,
                        null,
                        "between 4 and 5 requests per month",
                        null),
                effort(ProcessEffortEvidenceQuantityStatus.EXACT, "2", "item-1", "request")));

        assertThat(result.volumeProjection()).isEmpty();
        assertThat(result.effortProjection()).isPresent();
        assertThat(result.composable()).isFalse();
    }

    @Test
    void approximateNeverBecomesProductionProjection() {
        ProcessAnalysisResult result = map(evidence(
                volume(ProcessEffortEvidenceQuantityStatus.EXACT, "80", "item-1", "request"),
                effort(ProcessEffortEvidenceQuantityStatus.APPROXIMATE, "2", "item-1", "request")));

        assertThat(result.volumeProjection()).isPresent();
        assertThat(result.effortProjection()).isEmpty();
        assertThat(result.composable()).isFalse();
    }

    @Test
    void absentNeverBecomesProjectionEvenWhenBusinessItemContextIsPopulated() {
        ProcessAnalysisResult result = map(evidence(
                volume(ProcessEffortEvidenceQuantityStatus.EXACT, "120", "item-1", "ticket"),
                new ProcessEffortEvidenceQuantity(
                        ProcessEffortEvidenceQuantityStatus.ABSENT,
                        null,
                        null,
                        null,
                        "item-1",
                        "ticket",
                        null,
                        ProcessEffortDurationUnit.MINUTE,
                        null,
                        "Time per ticket is not stated.")));

        assertThat(result.volumeProjection()).isPresent();
        assertThat(result.effortProjection()).isEmpty();
        assertThat(result.composable()).isFalse();
    }

    @Test
    void unsupportedUnitNeverBecomesProductionProjection() {
        ProcessAnalysisResult result = map(evidence(
                volume(ProcessEffortEvidenceQuantityStatus.UNSUPPORTED_UNIT, "12", "item-1", "request"),
                effort(ProcessEffortEvidenceQuantityStatus.UNSUPPORTED_UNIT, "2", "item-1", "request")));

        assertThat(result.volumeProjection()).isEmpty();
        assertThat(result.effortProjection()).isEmpty();
        assertThat(result.composable()).isFalse();
        assertThat(result.sourceKnowledge().knownFacts()).isEmpty();
    }

    @Test
    void absenceOfBusinessItemRefBlocksProjection() {
        ProcessAnalysisResult result = map(evidence(
                volume(ProcessEffortEvidenceQuantityStatus.EXACT, "4000", null, "request"),
                effort(ProcessEffortEvidenceQuantityStatus.EXACT, "2", " ", "request")));

        assertThat(result.volumeProjection()).isEmpty();
        assertThat(result.effortProjection()).isEmpty();
        assertThat(result.composable()).isFalse();
        assertThat(result.sourceKnowledge().knownFacts()).isEmpty();
    }

    @Test
    void nonProcessAnalysisStatusesProduceNoUsableEffortProjections() {
        ProcessAnalysisModelResult modelResult = new ProcessAnalysisModelResult(
                understanding(ProcessAnalysisStatus.OUT_OF_SCOPE),
                evidence(
                        volume(ProcessEffortEvidenceQuantityStatus.EXACT, "4000", "item-1", "request"),
                        effort(ProcessEffortEvidenceQuantityStatus.EXACT, "2", "item-1", "request")));

        ProcessAnalysisResult result = mapper.map(modelResult);

        assertThat(result.volumeProjection()).isEmpty();
        assertThat(result.effortProjection()).isEmpty();
        assertThat(result.composable()).isFalse();
    }

    private ProcessAnalysisResult map(ProcessEffortEvidence evidence) {
        return mapper.map(new ProcessAnalysisModelResult(understanding(ProcessAnalysisStatus.PROCESS_IDENTIFIED), evidence));
    }

    private ProcessEffortEvidence evidence(
            ProcessEffortEvidenceQuantity volume,
            ProcessEffortEvidenceQuantity effort) {
        return new ProcessEffortEvidence(volume, effort);
    }

    private ProcessEffortEvidenceQuantity volume(
            ProcessEffortEvidenceQuantityStatus status,
            String magnitude,
            String businessItemRef,
            String businessItemLabel) {
        return quantity(
                status,
                magnitude,
                businessItemRef,
                businessItemLabel,
                ProcessReportingPeriodUnit.MONTH,
                null);
    }

    private ProcessEffortEvidenceQuantity effort(
            ProcessEffortEvidenceQuantityStatus status,
            String magnitude,
            String businessItemRef,
            String businessItemLabel) {
        return quantity(
                status,
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

    private ProcessEffortEvidenceQuantity volumeWithRangeFields(
            String magnitude,
            String minMagnitude,
            String maxMagnitude) {
        return new ProcessEffortEvidenceQuantity(
                ProcessEffortEvidenceQuantityStatus.EXACT,
                new BigDecimal(magnitude),
                minMagnitude == null ? null : new BigDecimal(minMagnitude),
                maxMagnitude == null ? null : new BigDecimal(maxMagnitude),
                "item-1",
                "request",
                ProcessReportingPeriodUnit.MONTH,
                null,
                null,
                null);
    }

    private ProcessEffortEvidenceQuantity effortWithRangeFields(
            String magnitude,
            String minMagnitude,
            String maxMagnitude) {
        return new ProcessEffortEvidenceQuantity(
                ProcessEffortEvidenceQuantityStatus.EXACT,
                new BigDecimal(magnitude),
                minMagnitude == null ? null : new BigDecimal(minMagnitude),
                maxMagnitude == null ? null : new BigDecimal(maxMagnitude),
                "item-1",
                "request",
                null,
                ProcessEffortDurationUnit.MINUTE,
                null,
                null);
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
