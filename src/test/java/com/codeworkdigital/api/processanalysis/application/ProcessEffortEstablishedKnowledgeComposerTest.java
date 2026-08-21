package com.codeworkdigital.api.processanalysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeworkdigital.api.processanalysis.domain.ProcessAnalysisKnowledge;
import com.codeworkdigital.api.processanalysis.domain.ProcessAnalysisScope;
import com.codeworkdigital.api.processanalysis.domain.ProcessBusinessItemPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessBusinessItemUnitId;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerBusinessItemUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceArtifact;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceArtifactId;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceArtifactKind;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceBase;
import com.codeworkdigital.api.processanalysis.domain.ProcessFactGrounding;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFact;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFactId;
import com.codeworkdigital.api.processanalysis.domain.ProcessQuantityProjection;
import com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProcessEffortEstablishedKnowledgeComposerTest {

    private final ProcessEffortEstablishedKnowledgeComposer composer =
            new ProcessEffortEstablishedKnowledgeComposer();
    private final ProcessEffortPerReportingPeriodMaterializer materializer =
            new ProcessEffortPerReportingPeriodMaterializer();

    @Test
    void originalOnlyVolumeAndEffortComposeSuccessfully() {
        ProcessEffortEstablishedKnowledge established = compose(
                sourceKnowledge(List.of(sourceVolume("4000"), sourceEffort("2"))),
                emptyClarificationKnowledge());

        assertThat(established.knownFacts())
                .extracting(ProcessKnownFact::id)
                .containsExactly(
                        ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
                        ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID);
        assertThat(established.evidenceBase().artifacts())
                .extracting(ProcessEvidenceArtifact::id)
                .containsExactly(ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID);
    }

    @Test
    void originalEffortAndClarificationVolumeComposeSuccessfully() {
        ProcessEffortEstablishedKnowledge established = compose(
                sourceKnowledge(List.of(sourceEffort("2"))),
                clarificationKnowledge(List.of(clarificationVolume("4000"))));

        assertThat(established.knownFacts())
                .extracting(ProcessKnownFact::id)
                .containsExactly(
                        ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
                        ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID);
        assertThat(established.knownFacts().get(0).evidenceArtifactIds())
                .containsExactly(ProcessEffortClarificationAnswerMaterializer.VOLUME_CLARIFICATION_ARTIFACT_ID);
        assertThat(established.knownFacts().get(1).evidenceArtifactIds())
                .containsExactly(ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID);
    }

    @Test
    void originalVolumeAndClarificationEffortComposeSuccessfully() {
        ProcessEffortEstablishedKnowledge established = compose(
                sourceKnowledge(List.of(sourceVolume("4000"))),
                clarificationKnowledge(List.of(clarificationEffort("2"))));

        assertThat(established.knownFacts())
                .extracting(ProcessKnownFact::id)
                .containsExactly(
                        ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
                        ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID);
        assertThat(established.knownFacts().get(0).evidenceArtifactIds())
                .containsExactly(ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID);
        assertThat(established.knownFacts().get(1).evidenceArtifactIds())
                .containsExactly(ProcessEffortClarificationAnswerMaterializer.EFFORT_CLARIFICATION_ARTIFACT_ID);
    }

    @Test
    void bothClarificationFactsComposeSuccessfully() {
        ProcessEffortEstablishedKnowledge established = compose(
                emptySourceKnowledge(),
                clarificationKnowledge(List.of(clarificationVolume("4000"), clarificationEffort("2"))));

        assertThat(established.knownFacts())
                .extracting(ProcessKnownFact::id)
                .containsExactly(
                        ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
                        ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID);
        assertThat(established.evidenceBase().artifacts())
                .extracting(ProcessEvidenceArtifact::id)
                .containsExactly(
                        ProcessEffortClarificationAnswerMaterializer.VOLUME_CLARIFICATION_ARTIFACT_ID,
                        ProcessEffortClarificationAnswerMaterializer.EFFORT_CLARIFICATION_ARTIFACT_ID);
    }

    @Test
    void deterministicFactAndArtifactOrderingAreStableForMixedSources() {
        ProcessEffortEstablishedKnowledge established = compose(
                sourceKnowledge(List.of(sourceEffort("2"))),
                clarificationKnowledge(List.of(clarificationVolume("4000"))));

        assertThat(established.knownFacts())
                .extracting(ProcessKnownFact::id)
                .containsExactly(
                        ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
                        ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID);
        assertThat(established.evidenceBase().artifacts())
                .extracting(ProcessEvidenceArtifact::id)
                .containsExactly(
                        ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID,
                        ProcessEffortClarificationAnswerMaterializer.VOLUME_CLARIFICATION_ARTIFACT_ID);
    }

    @Test
    void factEvidenceIdsAndProvenanceArePreservedExactly() {
        ProcessEffortEstablishedKnowledge established = compose(
                sourceKnowledge(List.of(sourceEffort("2"))),
                clarificationKnowledge(List.of(clarificationVolume("4000"))));

        ProcessKnownFact volume = established.knownFacts().get(0);
        ProcessKnownFact effort = established.knownFacts().get(1);
        assertThat(volume.evidenceArtifactIds())
                .containsExactly(ProcessEffortClarificationAnswerMaterializer.VOLUME_CLARIFICATION_ARTIFACT_ID)
                .doesNotContain(ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID);
        assertThat(effort.evidenceArtifactIds())
                .containsExactly(ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID)
                .doesNotContain(ProcessEffortClarificationAnswerMaterializer.EFFORT_CLARIFICATION_ARTIFACT_ID);
    }

    @Test
    void duplicateArtifactIdAcrossSourceAndClarificationIsRejected() {
        assertThatThrownBy(() -> compose(
                        sourceKnowledge(List.of(sourceEffort("2"))),
                        clarificationKnowledge(
                                List.of(clarificationVolume("4000")),
                                List.of(
                                        artifact(
                                                ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID,
                                                "Clarification artifact using source identity"),
                                        volumeClarificationArtifact()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate established effort evidence artifact id: source-process-description");
    }

    @Test
    void structurallyEqualDuplicateArtifactAcrossSourceAndClarificationIsRejected() {
        ProcessEvidenceArtifact duplicateSourceArtifact = artifact(
                ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID,
                "Process description submitted for this analysis");

        assertThatThrownBy(() -> compose(
                        sourceKnowledgeWithOriginalArtifactButNoFacts(),
                        clarificationKnowledge(List.of(), List.of(duplicateSourceArtifact))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate established effort evidence artifact id: source-process-description");
    }

    @Test
    void duplicateArtifactIdAcrossSourceAndClarificationWithDifferentDescriptionsIsRejected() {
        assertThatThrownBy(() -> compose(
                        sourceKnowledgeWithOriginalArtifactButNoFacts(),
                        clarificationKnowledge(
                                List.of(),
                                List.of(artifact(
                                        ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID,
                                        "Different clarification description")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate established effort evidence artifact id: source-process-description");
    }

    @Test
    void unreferencedDuplicateArtifactIdAcrossSourceAndClarificationIsRejected() {
        assertThatThrownBy(() -> compose(
                        sourceKnowledgeWithOriginalArtifactButNoFacts(),
                        clarificationKnowledge(
                                List.of(clarificationVolume("4000")),
                                List.of(
                                        artifact(
                                                ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID,
                                                "Unreferenced duplicate source identity"),
                                        volumeClarificationArtifact()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate established effort evidence artifact id: source-process-description");
    }

    @Test
    void duplicateVolumeFactAcrossSourceAndClarificationIsRejected() {
        assertThatThrownBy(() -> compose(
                        sourceKnowledge(List.of(sourceVolume("4000"))),
                        clarificationKnowledge(List.of(clarificationVolume("4000")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate established effort fact id: fact-volume-per-reporting-period");
    }

    @Test
    void duplicateEffortFactAcrossSourceAndClarificationIsRejected() {
        assertThatThrownBy(() -> compose(
                        sourceKnowledge(List.of(sourceEffort("2"))),
                        clarificationKnowledge(List.of(clarificationEffort("2")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate established effort fact id: fact-effort-per-business-item");
    }

    @Test
    void numericallyEqualDuplicateStillRejectsAsConflict() {
        assertThatThrownBy(() -> compose(
                        sourceKnowledge(List.of(sourceVolume("4000"))),
                        clarificationKnowledge(List.of(clarificationVolume("4000")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate established effort fact id");
    }

    @Test
    void establishedKnowledgeRejectsUnsupportedFactId() {
        assertThatThrownBy(() -> new ProcessEffortEstablishedKnowledge(
                        new ProcessEvidenceBase(List.of(sourceArtifact())),
                        List.of(sourceFact(
                                new ProcessKnownFactId("fact-unsupported"),
                                "1",
                                volumeUnit(),
                                ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported established effort fact id");
    }

    @Test
    void establishedKnowledgeRejectsNonSourceStatedFact() {
        ProcessKnownFact fact = new ProcessKnownFact(
                ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
                "derived",
                ProcessFactGrounding.DETERMINISTICALLY_DERIVED,
                ProcessAnalysisScope.processWide(),
                List.of(ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID),
                List.of(),
                Optional.of(new ProcessQuantityProjection(new BigDecimal("4000"), volumeUnit())));

        assertThatThrownBy(() -> new ProcessEffortEstablishedKnowledge(
                        new ProcessEvidenceBase(List.of(sourceArtifact())),
                        List.of(fact)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("established effort facts must be source stated");
    }

    @Test
    void establishedKnowledgeRejectsNonProcessWideFact() {
        ProcessKnownFact fact = new ProcessKnownFact(
                ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
                "volume",
                ProcessFactGrounding.SOURCE_STATED,
                ProcessAnalysisScope.operation("receive-ticket"),
                List.of(),
                List.of(ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID),
                Optional.of(new ProcessQuantityProjection(new BigDecimal("4000"), volumeUnit())));

        assertThatThrownBy(() -> new ProcessEffortEstablishedKnowledge(
                        new ProcessEvidenceBase(List.of(sourceArtifact())),
                        List.of(fact)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("process-wide");
    }

    @Test
    void establishedKnowledgeRejectsFactReferencingAbsentArtifact() {
        assertThatThrownBy(() -> new ProcessEffortEstablishedKnowledge(
                        new ProcessEvidenceBase(List.of(sourceArtifact())),
                        List.of(clarificationVolume("4000"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown evidence artifact");
    }

    @Test
    void unreferencedOriginalArtifactIsNotRequiredInComposedEvidenceBase() {
        ProcessEffortEstablishedKnowledge established = compose(
                sourceKnowledgeWithOriginalArtifactButNoFacts(),
                clarificationKnowledge(List.of(clarificationVolume("4000"))));

        assertThat(established.evidenceBase().artifacts())
                .extracting(ProcessEvidenceArtifact::id)
                .containsExactly(ProcessEffortClarificationAnswerMaterializer.VOLUME_CLARIFICATION_ARTIFACT_ID);
    }

    @Test
    void originalEffortAndClarificationVolumeMaterializeEightThousandMinutesPerMonth() {
        ProcessEffortDerivedResult result = materialize(compose(
                sourceKnowledge(List.of(sourceEffort("2"))),
                clarificationKnowledge(List.of(clarificationVolume("4000")))));

        assertDerivedEightThousandMinutesPerMonth(result);
    }

    @Test
    void originalVolumeAndClarificationEffortMaterializeEightThousandMinutesPerMonth() {
        ProcessEffortDerivedResult result = materialize(compose(
                sourceKnowledge(List.of(sourceVolume("4000"))),
                clarificationKnowledge(List.of(clarificationEffort("2")))));

        assertDerivedEightThousandMinutesPerMonth(result);
    }

    @Test
    void clarificationOnlyOperandsMaterializeEightThousandMinutesPerMonth() {
        ProcessEffortDerivedResult result = materialize(compose(
                emptySourceKnowledge(),
                clarificationKnowledge(List.of(clarificationVolume("4000"), clarificationEffort("2")))));

        assertDerivedEightThousandMinutesPerMonth(result);
    }

    @Test
    void mixedSourceFactsDerivedResultAndDerivationConstructValidProcessAnalysisKnowledge() {
        ProcessEffortEstablishedKnowledge established = compose(
                sourceKnowledge(List.of(sourceEffort("2"))),
                clarificationKnowledge(List.of(clarificationVolume("4000"))));
        ProcessEffortDerivedResult result = materialize(established);

        assertThatCode(() -> new ProcessAnalysisKnowledge(
                List.of(
                        established.knownFacts().get(0),
                        established.knownFacts().get(1),
                        result.resultFact()),
                List.of(),
                List.of(),
                List.of(),
                List.of(result.derivation())))
                .doesNotThrowAnyException();
        assertThat(result.resultFact().evidenceArtifactIds()).isEmpty();
    }

    @Test
    void productionRuntimeStillCallsModelAnalyzeExactlyOnce() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/application/ProcessAnalysisApplicationService.java"));

        assertThat(source.split("modelClient\\.analyze\\(", -1).length - 1).isEqualTo(1);
    }

    @Test
    void productionP06MultiplicationRemainsImplementedOnce() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/application/ProcessEffortPerReportingPeriodMaterializer.java"));

        assertThat(source.split("\\.multiply\\(", -1).length - 1).isEqualTo(1);
    }

    @Test
    void productionRuntimeTypesStillDoNotDependOnDerivationVerifier() {
        assertThat(declaredFieldTypes(ProcessAnalysisApplicationService.class))
                .doesNotContain(ProcessEffortPerReportingPeriodDerivationVerifier.class);
        assertThat(constructorParameterTypes(ProcessAnalysisApplicationService.class))
                .doesNotContain(ProcessEffortPerReportingPeriodDerivationVerifier.class);
        assertThat(declaredFieldTypes(ProcessEffortPerReportingPeriodMaterializer.class))
                .doesNotContain(ProcessEffortPerReportingPeriodDerivationVerifier.class);
        assertThat(constructorParameterTypes(ProcessEffortPerReportingPeriodMaterializer.class))
                .doesNotContain(ProcessEffortPerReportingPeriodDerivationVerifier.class);
        assertThat(declaredFieldTypes(ProcessEffortEstablishedKnowledgeComposer.class))
                .doesNotContain(ProcessEffortPerReportingPeriodDerivationVerifier.class);
        assertThat(constructorParameterTypes(ProcessEffortEstablishedKnowledgeComposer.class))
                .doesNotContain(ProcessEffortPerReportingPeriodDerivationVerifier.class);
    }

    private ProcessEffortEstablishedKnowledge compose(
            ProcessEffortSourceKnowledge sourceKnowledge,
            ProcessEffortClarificationKnowledge clarificationKnowledge) {
        return composer.compose(sourceKnowledge, clarificationKnowledge);
    }

    private ProcessEffortDerivedResult materialize(ProcessEffortEstablishedKnowledge establishedKnowledge) {
        return materializer.materialize(establishedKnowledge).orElseThrow();
    }

    private void assertDerivedEightThousandMinutesPerMonth(ProcessEffortDerivedResult result) {
        ProcessKnownFact fact = result.resultFact();
        ProcessQuantityProjection projection =
                (ProcessQuantityProjection) fact.computableProjection().orElseThrow();

        assertThat(projection.magnitude()).isEqualByComparingTo("8000");
        assertThat(projection.unit()).isInstanceOfSatisfying(
                ProcessEffortPerReportingPeriodUnit.class,
                unit -> {
                    assertThat(unit.effortDuration()).isEqualTo(ProcessEffortDurationUnit.MINUTE);
                    assertThat(unit.reportingPeriod()).isEqualTo(ProcessReportingPeriodUnit.MONTH);
                });
        assertThat(fact.grounding()).isEqualTo(ProcessFactGrounding.DETERMINISTICALLY_DERIVED);
        assertThat(fact.premiseFactIds()).containsExactly(
                ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
                ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID);
        assertThat(fact.evidenceArtifactIds()).isEmpty();
    }

    private ProcessEffortSourceKnowledge sourceKnowledge(List<ProcessKnownFact> facts) {
        return new ProcessEffortSourceKnowledge(new ProcessEvidenceBase(List.of(sourceArtifact())), facts);
    }

    private ProcessEffortSourceKnowledge sourceKnowledgeWithOriginalArtifactButNoFacts() {
        return new ProcessEffortSourceKnowledge(new ProcessEvidenceBase(List.of(sourceArtifact())), List.of());
    }

    private ProcessEffortSourceKnowledge emptySourceKnowledge() {
        return new ProcessEffortSourceKnowledge(new ProcessEvidenceBase(List.of()), List.of());
    }

    private ProcessEffortClarificationKnowledge clarificationKnowledge(List<ProcessKnownFact> facts) {
        return new ProcessEffortClarificationKnowledge(
                new ProcessEvidenceBase(List.of(volumeClarificationArtifact(), effortClarificationArtifact())),
                facts);
    }

    private ProcessEffortClarificationKnowledge clarificationKnowledge(
            List<ProcessKnownFact> facts,
            List<ProcessEvidenceArtifact> artifacts) {
        return new ProcessEffortClarificationKnowledge(new ProcessEvidenceBase(artifacts), facts);
    }

    private ProcessEffortClarificationKnowledge emptyClarificationKnowledge() {
        return new ProcessEffortClarificationKnowledge(new ProcessEvidenceBase(List.of()), List.of());
    }

    private ProcessKnownFact sourceVolume(String magnitude) {
        return sourceFact(
                ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
                magnitude,
                volumeUnit(),
                ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID);
    }

    private ProcessKnownFact sourceEffort(String magnitude) {
        return sourceFact(
                ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID,
                magnitude,
                effortUnit(),
                ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID);
    }

    private ProcessKnownFact clarificationVolume(String magnitude) {
        return sourceFact(
                ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
                magnitude,
                volumeUnit(),
                ProcessEffortClarificationAnswerMaterializer.VOLUME_CLARIFICATION_ARTIFACT_ID);
    }

    private ProcessKnownFact clarificationEffort(String magnitude) {
        return sourceFact(
                ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID,
                magnitude,
                effortUnit(),
                ProcessEffortClarificationAnswerMaterializer.EFFORT_CLARIFICATION_ARTIFACT_ID);
    }

    private ProcessKnownFact sourceFact(
            ProcessKnownFactId id,
            String magnitude,
            com.codeworkdigital.api.processanalysis.domain.ProcessQuantityUnitExpression unit,
            ProcessEvidenceArtifactId artifactId) {
        return new ProcessKnownFact(
                id,
                "source stated fact",
                ProcessFactGrounding.SOURCE_STATED,
                ProcessAnalysisScope.processWide(),
                List.of(),
                List.of(artifactId),
                Optional.of(new ProcessQuantityProjection(new BigDecimal(magnitude), unit)));
    }

    private ProcessBusinessItemPerReportingPeriodUnit volumeUnit() {
        return new ProcessBusinessItemPerReportingPeriodUnit(
                new ProcessBusinessItemUnitId("ticket"),
                ProcessReportingPeriodUnit.MONTH);
    }

    private ProcessEffortPerBusinessItemUnit effortUnit() {
        return new ProcessEffortPerBusinessItemUnit(
                ProcessEffortDurationUnit.MINUTE,
                new ProcessBusinessItemUnitId("ticket"));
    }

    private ProcessEvidenceArtifact sourceArtifact() {
        return artifact(
                ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID,
                "Process description submitted for this analysis");
    }

    private ProcessEvidenceArtifact volumeClarificationArtifact() {
        return artifact(
                ProcessEffortClarificationAnswerMaterializer.VOLUME_CLARIFICATION_ARTIFACT_ID,
                "Self-reported clarification answer for P06 volume per reporting period");
    }

    private ProcessEvidenceArtifact effortClarificationArtifact() {
        return artifact(
                ProcessEffortClarificationAnswerMaterializer.EFFORT_CLARIFICATION_ARTIFACT_ID,
                "Self-reported clarification answer for P06 effort per business item");
    }

    private ProcessEvidenceArtifact artifact(ProcessEvidenceArtifactId id, String description) {
        return new ProcessEvidenceArtifact(
                id,
                ProcessEvidenceArtifactKind.SOURCE_MATERIAL,
                description);
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
