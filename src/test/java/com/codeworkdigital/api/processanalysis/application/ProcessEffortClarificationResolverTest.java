package com.codeworkdigital.api.processanalysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeworkdigital.api.processanalysis.domain.ProcessAnalysisScope;
import com.codeworkdigital.api.processanalysis.domain.ProcessBusinessItemPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessBusinessItemUnitId;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceGap;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceSource;
import com.codeworkdigital.api.processanalysis.domain.ProcessFactGrounding;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFact;
import com.codeworkdigital.api.processanalysis.domain.ProcessQuantityProjection;
import com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProcessEffortClarificationResolverTest {

    private final ProcessEffortEvidenceProjectionMapper sourceMapper = new ProcessEffortEvidenceProjectionMapper();
    private final ProcessEffortClarificationResolver resolver = new ProcessEffortClarificationResolver(
            new ProcessEffortClarificationAnswerMaterializer(),
            new ProcessEffortEstablishedKnowledgeComposer(),
            new ProcessEffortPerReportingPeriodMaterializer(),
            new ProcessEffortMaterialityAssessmentEvaluator());

    @Test
    void volumeOnlyCompleteResolutionProducesEightThousandAndOpportunity() {
        ProcessEffortClarificationResolution resolution = resolver.resolve(
                context(baseline(evidence(absent("ticket", "ticket"), exactEffort("2", "ticket", "ticket")),
                        gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD))),
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000")));

        assertThat(quantity(resolution.derivedResult()).magnitude()).isEqualByComparingTo("8000");
        assertThat(resolution.materialityAssessment().status())
                .isEqualTo(ProcessEffortMaterialityAssessmentStatus.OPPORTUNITY_IDENTIFIED);
        assertThat(volumeFact(resolution.clarificationKnowledge()).computableProjection())
                .contains(new ProcessQuantityProjection(
                        new BigDecimal("4000"),
                        new ProcessBusinessItemPerReportingPeriodUnit(
                                new ProcessBusinessItemUnitId("ticket"),
                                ProcessReportingPeriodUnit.MONTH)));
    }

    @Test
    void effortOnlyCompleteResolutionProducesEightThousandAndOpportunity() {
        ProcessEffortClarificationResolution resolution = resolver.resolve(
                context(baseline(evidence(exactVolume("4000", "ticket", "ticket"), absent("ticket", "ticket")),
                        gap(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM))),
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM, "2")));

        assertThat(quantity(resolution.derivedResult()).magnitude()).isEqualByComparingTo("8000");
        assertThat(resolution.materialityAssessment().status())
                .isEqualTo(ProcessEffortMaterialityAssessmentStatus.OPPORTUNITY_IDENTIFIED);
    }

    @Test
    void bothGapsCompleteResolutionProducesEightThousandAndOpportunity() {
        ProcessEffortClarificationResolution resolution = resolver.resolve(
                context(baseline(evidence(absent("ticket", "ticket"), absent("ticket", "ticket")),
                        gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD),
                        gap(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM))),
                List.of(
                        answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000"),
                        answer(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM, "2")));

        assertThat(quantity(resolution.derivedResult()).magnitude()).isEqualByComparingTo("8000");
        assertThat(resolution.materialityAssessment().status())
                .isEqualTo(ProcessEffortMaterialityAssessmentStatus.OPPORTUNITY_IDENTIFIED);
    }

    @Test
    void belowThresholdResolutionDoesNotIdentifyMaterialJustification() {
        ProcessEffortClarificationResolution resolution = resolver.resolve(
                context(baseline(evidence(absent("ticket", "ticket"), exactEffort("2", "ticket", "ticket")),
                        gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD))),
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4")));

        assertThat(quantity(resolution.derivedResult()).magnitude()).isEqualByComparingTo("8");
        assertThat(resolution.materialityAssessment().status())
                .isEqualTo(ProcessEffortMaterialityAssessmentStatus.NO_MATERIAL_JUSTIFICATION_IDENTIFIED);
    }

    @Test
    void exactThresholdResolutionIdentifiesOpportunity() {
        ProcessEffortClarificationResolution resolution = resolver.resolve(
                context(baseline(evidence(absent("ticket", "ticket"), exactEffort("2", "ticket", "ticket")),
                        gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD))),
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "1200")));

        assertThat(quantity(resolution.derivedResult()).magnitude()).isEqualByComparingTo("2400");
        assertThat(resolution.materialityAssessment().status())
                .isEqualTo(ProcessEffortMaterialityAssessmentStatus.OPPORTUNITY_IDENTIFIED);
    }

    @Test
    void zeroResolutionRemainsValidAndNotMaterial() {
        ProcessEffortClarificationResolution resolution = resolver.resolve(
                context(baseline(evidence(absent("ticket", "ticket"), exactEffort("2", "ticket", "ticket")),
                        gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD))),
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "0")));

        assertThat(quantity(resolution.derivedResult()).magnitude()).isEqualByComparingTo("0");
        assertThat(resolution.materialityAssessment().status())
                .isEqualTo(ProcessEffortMaterialityAssessmentStatus.NO_MATERIAL_JUSTIFICATION_IDENTIFIED);
    }

    @Test
    void decimalAnswerRemainsValid() {
        ProcessEffortClarificationResolution resolution = resolver.resolve(
                context(baseline(evidence(exactVolume("1000", "ticket", "ticket"), absent("ticket", "ticket")),
                        gap(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM))),
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM, "2.5")));

        assertThat(quantity(resolution.derivedResult()).magnitude()).isEqualByComparingTo("2500.0");
        assertThat(resolution.materialityAssessment().status())
                .isEqualTo(ProcessEffortMaterialityAssessmentStatus.OPPORTUNITY_IDENTIFIED);
    }

    @Test
    void resolutionCarriesClarificationEstablishedAndMixedProvenance() {
        ProcessEffortClarificationResolution resolution = resolver.resolve(
                context(baseline(evidence(absent("ticket", "ticket"), exactEffort("2", "ticket", "ticket")),
                        gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD))),
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000")));

        assertThat(resolution.clarificationKnowledge().knownFacts())
                .singleElement()
                .satisfies(fact -> {
                    assertThat(fact.grounding()).isEqualTo(ProcessFactGrounding.SOURCE_STATED);
                    assertThat(fact.evidenceArtifactIds())
                            .containsExactly(ProcessEffortClarificationAnswerMaterializer.VOLUME_CLARIFICATION_ARTIFACT_ID);
                });
        assertThat(resolution.establishedKnowledge().knownFacts())
                .extracting(ProcessKnownFact::id)
                .containsExactly(
                        ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
                        ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID);
        assertThat(resolution.establishedKnowledge().knownFacts().get(0).evidenceArtifactIds())
                .containsExactly(ProcessEffortClarificationAnswerMaterializer.VOLUME_CLARIFICATION_ARTIFACT_ID);
        assertThat(resolution.establishedKnowledge().knownFacts().get(1).evidenceArtifactIds())
                .containsExactly(ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID);
    }

    @Test
    void derivedFactCarriesDeterministicGroundingCanonicalPremisesAndNoDirectEvidenceArtifacts() {
        ProcessEffortClarificationResolution resolution = resolver.resolve(
                context(baseline(evidence(absent("ticket", "ticket"), exactEffort("2", "ticket", "ticket")),
                        gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD))),
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000")));

        ProcessKnownFact fact = resolution.derivedResult().resultFact();

        assertThat(fact.grounding()).isEqualTo(ProcessFactGrounding.DETERMINISTICALLY_DERIVED);
        assertThat(fact.premiseFactIds()).containsExactly(
                ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
                ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID);
        assertThat(fact.evidenceArtifactIds()).isEmpty();
    }

    @Test
    void successfulResolutionNeverReturnsNotEstablishedMateriality() {
        ProcessEffortClarificationResolution resolution = resolver.resolve(
                context(baseline(evidence(absent("ticket", "ticket"), exactEffort("2", "ticket", "ticket")),
                        gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD))),
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4")));

        assertThat(resolution.materialityAssessment().status())
                .isNotEqualTo(ProcessEffortMaterialityAssessmentStatus.NOT_ESTABLISHED);
    }

    @Test
    void actionableBaselineProducesMinimalClarificationContext() {
        ProcessAnalysisResult baseline = baseline(
                evidence(absent("ticket", "ticket"), exactEffort("2", "ticket", "ticket")),
                gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD));
        ProcessEffortMaterialityAssessment assessment = baseline.materialityAssessment().orElseThrow();

        ProcessEffortClarificationContext context = context(baseline);

        assertThat(context.effortEvidence()).isSameAs(baseline.effortEvidence());
        assertThat(context.sourceKnowledge()).isSameAs(baseline.sourceKnowledge());
        assertThat(context.materialityAssessment()).isSameAs(assessment);
        assertThat(context.materialityAssessment().status())
                .isEqualTo(ProcessEffortMaterialityAssessmentStatus.NOT_ESTABLISHED);
        assertThat(context.actionableGaps())
                .extracting(ProcessEffortMaterialityEvidenceGap::kind)
                .containsExactly(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD);
    }

    @Test
    void clarificationContextDoesNotCarryBroadBaselineOrTechnologyFitState() {
        assertThat(recordComponentNames(ProcessEffortClarificationContext.class))
                .containsExactly(
                        "effortEvidence",
                        "sourceKnowledge",
                        "materialityAssessment",
                        "actionableGaps")
                .doesNotContain(
                        "understanding",
                        "technologyFitAssessments",
                        "volumeProjection",
                        "effortProjection",
                        "derivedResult",
                        "composable");
        assertThat(recordComponentTypes(ProcessEffortClarificationContext.class))
                .doesNotContain(
                        ProcessUnderstanding.class,
                        TechnologyFitAssessment.class,
                        ProcessEffortDerivedResult.class);
    }

    @Test
    void contextDefensivelyCopiesActionableGaps() {
        ProcessAnalysisResult baseline = baseline(
                evidence(absent("ticket", "ticket"), exactEffort("2", "ticket", "ticket")),
                gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD));
        List<ProcessEffortMaterialityEvidenceGap> mutableGaps =
                new ArrayList<>(baseline.materialityEvidenceGaps());

        ProcessEffortClarificationContext context = new ProcessEffortClarificationContext(
                baseline.effortEvidence(),
                baseline.sourceKnowledge(),
                baseline.materialityAssessment().orElseThrow(),
                mutableGaps);

        mutableGaps.clear();

        assertThat(context.actionableGaps())
                .extracting(ProcessEffortMaterialityEvidenceGap::kind)
                .containsExactly(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD);
        assertThatThrownBy(() -> context.actionableGaps().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void missingAnswerIsRejected() {
        ProcessAnalysisResult baseline = baseline(
                evidence(absent("ticket", "ticket"), absent("ticket", "ticket")),
                gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD),
                gap(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM));

        assertThatThrownBy(() -> resolver.resolve(
                        context(baseline),
                        List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly match");
    }

    @Test
    void extraAnswerIsRejected() {
        ProcessAnalysisResult baseline = baseline(
                evidence(absent("ticket", "ticket"), exactEffort("2", "ticket", "ticket")),
                gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD));

        assertThatThrownBy(() -> resolver.resolve(
                        context(baseline),
                        List.of(
                                answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000"),
                                answer(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM, "2"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly match");
    }

    @Test
    void duplicateAnswerKindIsRejected() {
        ProcessAnalysisResult baseline = baseline(
                evidence(absent("ticket", "ticket"), exactEffort("2", "ticket", "ticket")),
                gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD));

        assertThatThrownBy(() -> resolver.resolve(
                        context(baseline),
                        List.of(
                                answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000"),
                                answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "5000"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate clarification answer kind");
    }

    @Test
    void answerKindWithoutMatchingActionableGapIsRejected() {
        ProcessAnalysisResult baseline = baseline(
                evidence(absent("ticket", "ticket"), exactEffort("2", "ticket", "ticket")),
                gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD));

        assertThatThrownBy(() -> resolver.resolve(
                        context(baseline),
                        List.of(answer(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM, "2"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly match");
    }

    @Test
    void noGapBaselineIsRejectedIncludingWhenAnswersAreSupplied() {
        ProcessAnalysisResult baseline = baseline(evidence(
                exactVolume("4000", "ticket", "ticket"),
                exactEffort("2", "ticket", "ticket")));

        assertThatThrownBy(() -> context(baseline))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actionable materiality evidence gaps");
    }

    @Test
    void duplicateActionableGapKindsAreRejectedByContext() {
        ProcessAnalysisResult baseline = baseline(
                evidence(absent("ticket", "ticket"), exactEffort("2", "ticket", "ticket")),
                gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD),
                gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD));

        assertThatThrownBy(() -> context(baseline))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate actionable gap kind");
    }

    @Test
    void missingMaterialityAssessmentIsRejected() {
        ProcessAnalysisResult baseline = withoutMateriality(baseline(
                evidence(absent("ticket", "ticket"), exactEffort("2", "ticket", "ticket")),
                gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD)));

        assertThatThrownBy(() -> context(baseline))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("materiality assessment");
    }

    @Test
    void alreadyEstablishedBaselineIsRejected() {
        ProcessAnalysisResult baseline = withMateriality(
                baseline(evidence(absent("ticket", "ticket"), exactEffort("2", "ticket", "ticket")),
                        gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD)),
                new ProcessEffortMaterialityAssessment(
                        ProcessEffortMaterialityAssessmentStatus.OPPORTUNITY_IDENTIFIED,
                        ProcessEffortMaterialityThreshold.P06_LAB_POLICY,
                        Optional.of(new ProcessQuantityProjection(
                                new BigDecimal("8000"),
                                new ProcessEffortPerReportingPeriodUnit(
                                        ProcessEffortDurationUnit.MINUTE,
                                        ProcessReportingPeriodUnit.MONTH)))));

        assertThatThrownBy(() -> context(baseline))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not established");
    }

    @Test
    void nonP06AssessmentThresholdIsRejectedByContext() {
        ProcessAnalysisResult baseline = withMateriality(
                baseline(evidence(absent("ticket", "ticket"), exactEffort("2", "ticket", "ticket")),
                        gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD)),
                ProcessEffortMaterialityAssessment.notEstablished(new ProcessEffortMaterialityThreshold(
                        new ProcessQuantityProjection(
                                new BigDecimal("2401"),
                                new ProcessEffortPerReportingPeriodUnit(
                                        ProcessEffortDurationUnit.MINUTE,
                                        ProcessReportingPeriodUnit.MONTH)))));

        assertThatThrownBy(() -> context(baseline))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("P06 lab policy");
    }

    @Test
    void nonProcessBaselineIsRejected() {
        ProcessAnalysisResult baseline = withUnderstanding(
                baseline(evidence(absent("ticket", "ticket"), exactEffort("2", "ticket", "ticket")),
                        gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD)),
                understanding(ProcessAnalysisStatus.OUT_OF_SCOPE));

        assertThatThrownBy(() -> context(baseline))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identify a process");
    }

    @Test
    void resolverRejectsAnswersWhenNoValidContextExists() {
        assertThatThrownBy(() -> resolver.resolve(
                        null,
                        List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000"))))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("context");
    }

    @Test
    void unanswerableActionableContextFailsClosedWhenClarificationKnowledgeCannotMaterialize() {
        ProcessAnalysisResult baseline = baseline(
                evidence(absent("ticket", " "), exactEffort("2", "ticket", "ticket")),
                gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD));

        assertThatThrownBy(() -> resolver.resolve(
                        context(baseline),
                        List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no answerable evidence context");
    }

    @Test
    void compositionConflictFailsClosed() {
        ProcessAnalysisResult baseline = withSourceKnowledge(
                baseline(evidence(absent("ticket", "ticket"), absent("ticket", "ticket")),
                        gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD),
                        gap(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM)),
                baseline(evidence(
                        exactVolume("4000", "ticket", "ticket"),
                        exactEffort("2", "ticket", "ticket"))).sourceKnowledge());

        assertThatThrownBy(() -> resolver.resolve(
                        context(baseline),
                        List.of(
                                answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000"),
                                answer(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM, "2"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate established effort fact id");
    }

    @Test
    void deterministicBurdenUnavailableAfterCompleteAnswersFailsClosed() {
        ProcessAnalysisResult baseline = baseline(
                evidence(absent("volume-item", "ticket"), exactEffort("2", "effort-item", "ticket")),
                gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD));

        assertThatThrownBy(() -> resolver.resolve(
                        context(baseline),
                        List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("did not produce deterministic effort burden");
    }

    @Test
    void originalBaselineEvidenceSourceKnowledgeAndGapListAreNotMutated() {
        ProcessAnalysisResult baseline = baseline(
                evidence(absent("ticket", "ticket"), exactEffort("2", "ticket", "ticket")),
                gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD));
        ProcessEffortEvidence originalEvidence = baseline.effortEvidence();
        ProcessEffortSourceKnowledge originalSourceKnowledge = baseline.sourceKnowledge();
        List<ProcessEffortMaterialityEvidenceGap> originalGaps = baseline.materialityEvidenceGaps();

        resolver.resolve(
                context(baseline),
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000")));

        assertThat(baseline.effortEvidence()).isSameAs(originalEvidence);
        assertThat(baseline.effortEvidence().volumePerReportingPeriod().status())
                .isEqualTo(ProcessEffortEvidenceQuantityStatus.ABSENT);
        assertThat(baseline.sourceKnowledge()).isSameAs(originalSourceKnowledge);
        assertThat(baseline.sourceKnowledge().knownFacts())
                .singleElement()
                .satisfies(fact -> assertThat(fact.evidenceArtifactIds())
                        .containsExactly(ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID));
        assertThat(baseline.materialityEvidenceGaps()).isSameAs(originalGaps);
        assertThat(baseline.materialityEvidenceGaps())
                .extracting(ProcessEffortMaterialityEvidenceGap::kind)
                .containsExactly(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD);
    }

    @Test
    void resolverDoesNotDependOnModelTechnologyFitGapIdentificationOrVerifier() {
        assertThat(declaredFieldTypes(ProcessEffortClarificationResolver.class))
                .doesNotContain(
                        ProcessAnalysisModelClient.class,
                        TechnologyFitAssessmentEvaluator.class,
                        ProcessEffortMaterialityEvidenceGapIdentifier.class,
                        ProcessEffortPerReportingPeriodDerivationVerifier.class);
        assertThat(constructorParameterTypes(ProcessEffortClarificationResolver.class))
                .doesNotContain(
                        ProcessAnalysisModelClient.class,
                        TechnologyFitAssessmentEvaluator.class,
                        ProcessEffortMaterialityEvidenceGapIdentifier.class,
                        ProcessEffortPerReportingPeriodDerivationVerifier.class);
        assertThat(recordComponentTypes(ProcessEffortClarificationContext.class))
                .doesNotContain(
                        ProcessAnalysisModelClient.class,
                        TechnologyFitAssessmentEvaluator.class,
                        ProcessEffortMaterialityEvidenceGapIdentifier.class,
                        ProcessEffortPerReportingPeriodDerivationVerifier.class);
    }

    @Test
    void resolverNoLongerAcceptsProcessAnalysisResult() {
        assertThat(methodParameterTypes(ProcessEffortClarificationResolver.class, "resolve"))
                .doesNotContain(ProcessAnalysisResult.class)
                .contains(ProcessEffortClarificationContext.class);
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
    void resolverDoesNotUseDerivationVerifier() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/application/ProcessEffortClarificationResolver.java"));

        assertThat(source).doesNotContain("DerivationVerifier");
    }

    @Test
    void publicApiDoesNotExposeClarificationContext() throws Exception {
        String controller = Files.readString(Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/api/ProcessAnalysisController.java"));
        String request = Files.readString(Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/api/ProcessAnalysisRequest.java"));
        String response = Files.readString(Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/api/ProcessAnalysisResponse.java"));

        assertThat(controller + request + response)
                .doesNotContain("ProcessEffortClarificationContext")
                .doesNotContain("ProcessEffortMaterialityClarificationAnswer");
    }

    private ProcessEffortClarificationContext context(ProcessAnalysisResult baseline) {
        return ProcessEffortClarificationContext.from(baseline);
    }

    private ProcessAnalysisResult baseline(
            ProcessEffortEvidence evidence,
            ProcessEffortMaterialityEvidenceGap... gaps) {
        ProcessAnalysisResult mapped = sourceMapper.map(new ProcessAnalysisModelResult(
                understanding(ProcessAnalysisStatus.PROCESS_IDENTIFIED),
                evidence));
        return new ProcessAnalysisResult(
                mapped.understanding(),
                mapped.effortEvidence(),
                mapped.volumeProjection(),
                mapped.effortProjection(),
                mapped.sourceKnowledge(),
                Optional.empty(),
                Optional.of(ProcessEffortMaterialityAssessment.notEstablished(
                        ProcessEffortMaterialityThreshold.P06_LAB_POLICY)),
                List.of(gaps),
                mapped.composable());
    }

    private ProcessAnalysisResult withoutMateriality(ProcessAnalysisResult baseline) {
        return new ProcessAnalysisResult(
                baseline.understanding(),
                baseline.effortEvidence(),
                baseline.volumeProjection(),
                baseline.effortProjection(),
                baseline.sourceKnowledge(),
                baseline.derivedResult(),
                Optional.empty(),
                baseline.materialityEvidenceGaps(),
                baseline.composable());
    }

    private ProcessAnalysisResult withMateriality(
            ProcessAnalysisResult baseline,
            ProcessEffortMaterialityAssessment materialityAssessment) {
        return new ProcessAnalysisResult(
                baseline.understanding(),
                baseline.effortEvidence(),
                baseline.volumeProjection(),
                baseline.effortProjection(),
                baseline.sourceKnowledge(),
                baseline.derivedResult(),
                Optional.of(materialityAssessment),
                baseline.materialityEvidenceGaps(),
                baseline.composable());
    }

    private ProcessAnalysisResult withUnderstanding(
            ProcessAnalysisResult baseline,
            ProcessUnderstanding understanding) {
        return new ProcessAnalysisResult(
                understanding,
                baseline.effortEvidence(),
                baseline.volumeProjection(),
                baseline.effortProjection(),
                baseline.sourceKnowledge(),
                baseline.derivedResult(),
                baseline.materialityAssessment(),
                baseline.materialityEvidenceGaps(),
                baseline.composable());
    }

    private ProcessAnalysisResult withSourceKnowledge(
            ProcessAnalysisResult baseline,
            ProcessEffortSourceKnowledge sourceKnowledge) {
        return new ProcessAnalysisResult(
                baseline.understanding(),
                baseline.effortEvidence(),
                baseline.volumeProjection(),
                baseline.effortProjection(),
                sourceKnowledge,
                baseline.derivedResult(),
                baseline.materialityAssessment(),
                baseline.materialityEvidenceGaps(),
                baseline.composable());
    }

    private ProcessKnownFact volumeFact(ProcessEffortClarificationKnowledge knowledge) {
        return knowledge.knownFacts().stream()
                .filter(fact -> fact.id().equals(ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID))
                .findFirst()
                .orElseThrow();
    }

    private ProcessQuantityProjection quantity(ProcessEffortDerivedResult derivedResult) {
        return (ProcessQuantityProjection) derivedResult.resultFact().computableProjection().orElseThrow();
    }

    private ProcessEffortMaterialityClarificationAnswer answer(
            ProcessEffortMaterialityEvidenceGapKind kind,
            String magnitude) {
        return new ProcessEffortMaterialityClarificationAnswer(kind, new BigDecimal(magnitude));
    }

    private ProcessEffortMaterialityEvidenceGap gap(ProcessEffortMaterialityEvidenceGapKind kind) {
        return new ProcessEffortMaterialityEvidenceGap(
                kind,
                new ProcessEvidenceGap(
                        "question",
                        ProcessEvidenceSource.SELF_REPORTED,
                        ProcessEffortMaterialityEvidenceGapIdentifier.DECISION_AFFECTED,
                        ProcessAnalysisScope.processWide()));
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

    private List<Class<?>> methodParameterTypes(Class<?> type, String methodName) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .map(method -> method.getParameterTypes())
                .flatMap(Arrays::stream)
                .toList();
    }

    private List<String> recordComponentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(component -> component.getName())
                .toList();
    }

    private List<Class<?>> recordComponentTypes(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .<Class<?>>map(component -> component.getType())
                .toList();
    }
}
