package com.codeworkdigital.api.processanalysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeworkdigital.api.processanalysis.domain.ProcessAnalysisScope;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceGap;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceSource;
import com.codeworkdigital.api.processanalysis.domain.ProcessQuantityProjection;
import com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProcessEffortMaterialityEvidenceGapIdentifierTest {

    private final ProcessEffortMaterialityEvidenceGapIdentifier identifier =
            new ProcessEffortMaterialityEvidenceGapIdentifier();

    @Test
    void productionIdentifierDoesNotRetainDuplicateQuestionKindType() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/application/"
                        + "ProcessEffortMaterialityEvidenceGapIdentifier.java"));

        assertThat(source).doesNotContain("QuestionKind");
    }

    @Test
    void absentVolumeOnlyProducesOneVolumeGap() {
        List<ProcessEffortMaterialityEvidenceGap> gaps = identify(
                processIdentified(),
                evidence(absent("item-1", "ticket"), exactEffort("2", "item-1", "ticket")),
                notEstablished(),
                ProcessAnalysisLocale.EN);

        assertThat(gaps).singleElement().satisfies(typedGap -> {
            assertThat(typedGap.kind())
                    .isEqualTo(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD);
            ProcessEvidenceGap gap = typedGap.evidenceGap();
            assertThat(gap.question())
                    .isEqualTo("What monthly quantity do you use as the reference volume for this process?");
            assertThat(gap.source()).isEqualTo(ProcessEvidenceSource.SELF_REPORTED);
            assertThat(gap.scope()).isEqualTo(ProcessAnalysisScope.processWide());
        });
    }

    @Test
    void absentEffortOnlyProducesOneEffortGap() {
        List<ProcessEffortMaterialityEvidenceGap> gaps = identify(
                processIdentified(),
                evidence(exactVolume("4000", "item-1", "ticket"), absent("item-1", "ticket")),
                notEstablished(),
                ProcessAnalysisLocale.EN);

        assertThat(gaps).singleElement().satisfies(typedGap -> {
            assertThat(typedGap.kind())
                    .isEqualTo(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM);
            assertThat(typedGap.evidenceGap().question())
                    .isEqualTo("How many minutes of effort per processed business item do you use as the reference value?");
        });
    }

    @Test
    void bothAbsentProduceVolumeThenEffortGaps() {
        List<ProcessEffortMaterialityEvidenceGap> gaps = identify(
                processIdentified(),
                evidence(absent("item-1", "ticket"), absent("item-1", "ticket")),
                notEstablished(),
                ProcessAnalysisLocale.EN);

        assertThat(gaps).hasSize(2);
        assertThat(gaps).extracting(ProcessEffortMaterialityEvidenceGap::kind)
                .containsExactly(
                        ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD,
                        ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM);
        assertThat(evidenceGaps(gaps)).extracting(ProcessEvidenceGap::question)
                .containsExactly(
                        "What monthly quantity do you use as the reference volume for this process?",
                        "How many minutes of effort per processed business item do you use as the reference value?");
    }

    @Test
    void generatedGapsUseSelfReportedProcessWideScopeAndStableDecisionAffected() {
        List<ProcessEffortMaterialityEvidenceGap> gaps = identify(
                processIdentified(),
                evidence(absent("item-1", "ticket"), absent("item-1", "ticket")),
                notEstablished(),
                ProcessAnalysisLocale.EN);

        assertThat(gaps).allSatisfy(typedGap -> {
            assertThat(typedGap.evidenceGap()).isNotNull();
            ProcessEvidenceGap gap = typedGap.evidenceGap();
            assertThat(gap.source()).isEqualTo(ProcessEvidenceSource.SELF_REPORTED);
            assertThat(gap.scope()).isEqualTo(ProcessAnalysisScope.processWide());
            assertThat(gap.decisionAffected())
                    .isEqualTo(ProcessEffortMaterialityEvidenceGapIdentifier.DECISION_AFFECTED);
            assertThat(gap.decisionAffected()).doesNotContain("2400", "40 hours", "INTERVENTION_JUSTIFIED");
        });
        assertThat(evidenceGaps(gaps)).extracting(ProcessEvidenceGap::decisionAffected).containsOnly(
                ProcessEffortMaterialityEvidenceGapIdentifier.DECISION_AFFECTED);
    }

    @Test
    void establishedMaterialityProducesNoGapsEvenWhenEvidenceIsAbsent() {
        List<ProcessEffortMaterialityEvidenceGap> gaps = identify(
                processIdentified(),
                evidence(absent("item-1", "ticket"), absent("item-1", "ticket")),
                establishedNoMaterialJustification(),
                ProcessAnalysisLocale.EN);

        assertThat(gaps).isEmpty();
    }

    @Test
    void nonProcessStatusesProduceNoP06MaterialityGaps() {
        for (ProcessAnalysisStatus status : List.of(
                ProcessAnalysisStatus.OUT_OF_SCOPE,
                ProcessAnalysisStatus.INSUFFICIENT_INFORMATION)) {
            List<ProcessEffortMaterialityEvidenceGap> gaps = identify(
                    understanding(status),
                    evidence(absent("item-1", "ticket"), absent("item-1", "ticket")),
                    notEstablished(),
                    ProcessAnalysisLocale.EN);

            assertThat(gaps).isEmpty();
        }
    }

    @Test
    void approximateVolumeWithExactEffortProducesVolumeGapOnly() {
        List<ProcessEffortMaterialityEvidenceGap> gaps = identify(
                processIdentified(),
                evidence(approximateVolume("4000", "item-1", "ticket"), exactEffort("2", "item-1", "ticket")),
                notEstablished(),
                ProcessAnalysisLocale.EN);

        assertThat(gaps).extracting(ProcessEffortMaterialityEvidenceGap::kind)
                .containsExactly(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD);
        assertThat(gaps.getFirst().evidenceGap().question())
                .isEqualTo("The description provides an approximate monthly volume. What exact monthly quantity do you want to use as the scalar reference value for this calculation?");
    }

    @Test
    void rangeVolumeWithExactEffortProducesVolumeGapOnly() {
        List<ProcessEffortMaterialityEvidenceGap> gaps = identify(
                processIdentified(),
                evidence(rangeVolume("3000", "5000", "item-1", "ticket"), exactEffort("2", "item-1", "ticket")),
                notEstablished(),
                ProcessAnalysisLocale.EN);

        assertThat(gaps).extracting(ProcessEffortMaterialityEvidenceGap::kind)
                .containsExactly(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD);
        assertThat(gaps.getFirst().evidenceGap().question())
                .isEqualTo("The description provides a range for monthly volume. What exact monthly quantity do you want to use as the scalar reference value for this calculation?");
    }

    @Test
    void exactVolumeWithApproximateEffortProducesEffortGapOnly() {
        List<ProcessEffortMaterialityEvidenceGap> gaps = identify(
                processIdentified(),
                evidence(exactVolume("4000", "item-1", "ticket"), approximateEffort("2", "item-1", "ticket")),
                notEstablished(),
                ProcessAnalysisLocale.EN);

        assertThat(gaps).extracting(ProcessEffortMaterialityEvidenceGap::kind)
                .containsExactly(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM);
        assertThat(gaps.getFirst().evidenceGap().question())
                .isEqualTo("The description provides an approximate effort per item. How many minutes per item do you want to use as the exact scalar reference value for this calculation?");
    }

    @Test
    void exactVolumeWithRangeEffortProducesEffortGapOnly() {
        List<ProcessEffortMaterialityEvidenceGap> gaps = identify(
                processIdentified(),
                evidence(exactVolume("4000", "item-1", "ticket"), rangeEffort("1", "3", "item-1", "ticket")),
                notEstablished(),
                ProcessAnalysisLocale.EN);

        assertThat(gaps).extracting(ProcessEffortMaterialityEvidenceGap::kind)
                .containsExactly(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM);
        assertThat(gaps.getFirst().evidenceGap().question())
                .isEqualTo("The description provides a range for effort per item. How many minutes per item do you want to use as the exact scalar reference value for this calculation?");
    }

    @Test
    void nonExactAndAbsentCombinationsProduceGapsInCanonicalOrder() {
        List<ProcessEffortEvidence> cases = List.of(
                evidence(rangeVolume("4", "5", "item-1", "ticket"), absent("item-1", "ticket")),
                evidence(approximateVolume("4", "item-1", "ticket"), absent("item-1", "ticket")),
                evidence(absent("item-1", "ticket"), rangeEffort("10", "15", "item-1", "ticket")),
                evidence(absent("item-1", "ticket"), approximateEffort("12", "item-1", "ticket")),
                evidence(approximateVolume("4", "item-1", "ticket"), rangeEffort("10", "15", "item-1", "ticket")),
                evidence(rangeVolume("4", "5", "item-1", "ticket"), approximateEffort("12", "item-1", "ticket")),
                evidence(approximateVolume("4", "item-1", "ticket"), approximateEffort("12", "item-1", "ticket")),
                evidence(rangeVolume("4", "5", "item-1", "ticket"), rangeEffort("10", "15", "item-1", "ticket")));

        for (ProcessEffortEvidence effortEvidence : cases) {
            List<ProcessEffortMaterialityEvidenceGap> gaps = identify(
                    processIdentified(),
                    effortEvidence,
                    notEstablished(),
                    ProcessAnalysisLocale.EN);

            assertThat(gaps).extracting(ProcessEffortMaterialityEvidenceGap::kind)
                    .containsExactly(
                            ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD,
                            ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM);
        }
    }

    @Test
    void unsupportedUnitRemainsNonActionable() {
        assertThat(identify(
                        processIdentified(),
                        evidence(unsupportedVolume("4000", "item-1", "ticket"), exactEffort("2", "item-1", "ticket")),
                        notEstablished(),
                        ProcessAnalysisLocale.EN))
                .isEmpty();
        assertThat(identify(
                        processIdentified(),
                        evidence(exactVolume("4000", "item-1", "ticket"), unsupportedEffort("2", "item-1", "ticket")),
                        notEstablished(),
                        ProcessAnalysisLocale.EN))
                .isEmpty();
        assertThat(identify(
                        processIdentified(),
                        evidence(absent("item-1", "ticket"), unsupportedEffort("2", "item-1", "ticket")),
                        notEstablished(),
                        ProcessAnalysisLocale.EN))
                .isEmpty();
    }

    @Test
    void absentVolumeWithMismatchedExactEffortRefProducesNoGaps() {
        List<ProcessEffortMaterialityEvidenceGap> gaps = identify(
                processIdentified(),
                evidence(absent("item-A", "ticket"), exactEffort("2", "item-B", "ticket")),
                notEstablished(),
                ProcessAnalysisLocale.EN);

        assertThat(gaps).isEmpty();
    }

    @Test
    void absentEffortWithMismatchedExactVolumeRefProducesNoGaps() {
        List<ProcessEffortMaterialityEvidenceGap> gaps = identify(
                processIdentified(),
                evidence(exactVolume("4000", "item-A", "ticket"), absent("item-B", "ticket")),
                notEstablished(),
                ProcessAnalysisLocale.EN);

        assertThat(gaps).isEmpty();
    }

    @Test
    void bothAbsentWithDifferentRefsProduceNoGaps() {
        List<ProcessEffortMaterialityEvidenceGap> gaps = identify(
                processIdentified(),
                evidence(absent("item-A", "ticket"), absent("item-B", "ticket")),
                notEstablished(),
                ProcessAnalysisLocale.EN);

        assertThat(gaps).isEmpty();
    }

    @Test
    void bothAbsentWithBlankOrNullRefsProduceNoGaps() {
        assertThat(identify(
                        processIdentified(),
                        evidence(absent(" ", "ticket"), absent(" ", "ticket")),
                        notEstablished(),
                        ProcessAnalysisLocale.EN))
                .isEmpty();
        assertThat(identify(
                        processIdentified(),
                        evidence(absent(null, "ticket"), absent(null, "ticket")),
                        notEstablished(),
                        ProcessAnalysisLocale.EN))
                .isEmpty();
    }

    @Test
    void missingLabelRequiredForFutureSourceStatedFactProducesNoGap() {
        assertThat(identify(
                        processIdentified(),
                        evidence(absent("item-1", "ticket"), exactEffort("2", "item-1", " ")),
                        notEstablished(),
                        ProcessAnalysisLocale.EN))
                .isEmpty();
        assertThat(identify(
                        processIdentified(),
                        evidence(exactVolume("4000", "item-1", null), absent("item-1", "ticket")),
                        notEstablished(),
                        ProcessAnalysisLocale.EN))
                .isEmpty();
        assertThat(identify(
                        processIdentified(),
                        evidence(absent("item-1", ""), absent("item-1", "ticket")),
                        notEstablished(),
                        ProcessAnalysisLocale.EN))
                .isEmpty();
        assertThat(identify(
                        processIdentified(),
                        evidence(approximateVolume("4", "item-1", ""), exactEffort("2", "item-1", "ticket")),
                        notEstablished(),
                        ProcessAnalysisLocale.EN))
                .isEmpty();
        assertThat(identify(
                        processIdentified(),
                        evidence(exactVolume("4000", "item-1", "ticket"), rangeEffort("1", "3", "item-1", null)),
                        notEstablished(),
                        ProcessAnalysisLocale.EN))
                .isEmpty();
    }

    @Test
    void malformedApproximateQuantitiesProduceNoGap() {
        List<ProcessEffortEvidenceQuantity> malformedVolumes = List.of(
                quantity(ProcessEffortEvidenceQuantityStatus.APPROXIMATE, null, "item-1", "ticket", ProcessReportingPeriodUnit.MONTH, null),
                quantity(ProcessEffortEvidenceQuantityStatus.APPROXIMATE, "-1", "item-1", "ticket", ProcessReportingPeriodUnit.MONTH, null),
                rangeQuantity(ProcessEffortEvidenceQuantityStatus.APPROXIMATE, "4", "5", "item-1", "ticket", ProcessReportingPeriodUnit.MONTH, null),
                quantity(ProcessEffortEvidenceQuantityStatus.APPROXIMATE, "4", "item-1", "ticket", null, null),
                quantity(ProcessEffortEvidenceQuantityStatus.APPROXIMATE, "4", "item-1", "ticket", ProcessReportingPeriodUnit.MONTH, ProcessEffortDurationUnit.MINUTE));
        for (ProcessEffortEvidenceQuantity volume : malformedVolumes) {
            assertThat(identify(
                            processIdentified(),
                            evidence(volume, exactEffort("2", "item-1", "ticket")),
                            notEstablished(),
                            ProcessAnalysisLocale.EN))
                    .isEmpty();
        }

        List<ProcessEffortEvidenceQuantity> malformedEfforts = List.of(
                quantity(ProcessEffortEvidenceQuantityStatus.APPROXIMATE, null, "item-1", "ticket", null, ProcessEffortDurationUnit.MINUTE),
                quantity(ProcessEffortEvidenceQuantityStatus.APPROXIMATE, "-1", "item-1", "ticket", null, ProcessEffortDurationUnit.MINUTE),
                rangeQuantity(ProcessEffortEvidenceQuantityStatus.APPROXIMATE, "1", "3", "item-1", "ticket", null, ProcessEffortDurationUnit.MINUTE),
                quantity(ProcessEffortEvidenceQuantityStatus.APPROXIMATE, "2", "item-1", "ticket", ProcessReportingPeriodUnit.MONTH, ProcessEffortDurationUnit.MINUTE),
                quantity(ProcessEffortEvidenceQuantityStatus.APPROXIMATE, "2", "item-1", "ticket", null, null));
        for (ProcessEffortEvidenceQuantity effort : malformedEfforts) {
            assertThat(identify(
                            processIdentified(),
                            evidence(exactVolume("4000", "item-1", "ticket"), effort),
                            notEstablished(),
                            ProcessAnalysisLocale.EN))
                    .isEmpty();
        }
    }

    @Test
    void malformedRangeQuantitiesProduceNoGap() {
        List<ProcessEffortEvidenceQuantity> malformedVolumes = List.of(
                new ProcessEffortEvidenceQuantity(ProcessEffortEvidenceQuantityStatus.RANGE, null, null, new BigDecimal("5"), "item-1", "ticket", ProcessReportingPeriodUnit.MONTH, null, null, null),
                new ProcessEffortEvidenceQuantity(ProcessEffortEvidenceQuantityStatus.RANGE, null, new BigDecimal("4"), null, "item-1", "ticket", ProcessReportingPeriodUnit.MONTH, null, null, null),
                rangeQuantity(ProcessEffortEvidenceQuantityStatus.RANGE, "-1", "5", "item-1", "ticket", ProcessReportingPeriodUnit.MONTH, null),
                rangeQuantity(ProcessEffortEvidenceQuantityStatus.RANGE, "5", "-1", "item-1", "ticket", ProcessReportingPeriodUnit.MONTH, null),
                rangeQuantity(ProcessEffortEvidenceQuantityStatus.RANGE, "5", "4", "item-1", "ticket", ProcessReportingPeriodUnit.MONTH, null),
                new ProcessEffortEvidenceQuantity(ProcessEffortEvidenceQuantityStatus.RANGE, new BigDecimal("4"), new BigDecimal("4"), new BigDecimal("5"), "item-1", "ticket", ProcessReportingPeriodUnit.MONTH, null, null, null));
        for (ProcessEffortEvidenceQuantity volume : malformedVolumes) {
            assertThat(identify(
                            processIdentified(),
                            evidence(volume, exactEffort("2", "item-1", "ticket")),
                            notEstablished(),
                            ProcessAnalysisLocale.EN))
                    .isEmpty();
        }

        List<ProcessEffortEvidenceQuantity> malformedEfforts = List.of(
                new ProcessEffortEvidenceQuantity(ProcessEffortEvidenceQuantityStatus.RANGE, null, null, new BigDecimal("3"), "item-1", "ticket", null, ProcessEffortDurationUnit.MINUTE, null, null),
                new ProcessEffortEvidenceQuantity(ProcessEffortEvidenceQuantityStatus.RANGE, null, new BigDecimal("1"), null, "item-1", "ticket", null, ProcessEffortDurationUnit.MINUTE, null, null),
                rangeQuantity(ProcessEffortEvidenceQuantityStatus.RANGE, "-1", "3", "item-1", "ticket", null, ProcessEffortDurationUnit.MINUTE),
                rangeQuantity(ProcessEffortEvidenceQuantityStatus.RANGE, "3", "-1", "item-1", "ticket", null, ProcessEffortDurationUnit.MINUTE),
                rangeQuantity(ProcessEffortEvidenceQuantityStatus.RANGE, "3", "1", "item-1", "ticket", null, ProcessEffortDurationUnit.MINUTE),
                new ProcessEffortEvidenceQuantity(ProcessEffortEvidenceQuantityStatus.RANGE, new BigDecimal("2"), new BigDecimal("1"), new BigDecimal("3"), "item-1", "ticket", null, ProcessEffortDurationUnit.MINUTE, null, null));
        for (ProcessEffortEvidenceQuantity effort : malformedEfforts) {
            assertThat(identify(
                            processIdentified(),
                            evidence(exactVolume("4000", "item-1", "ticket"), effort),
                            notEstablished(),
                            ProcessAnalysisLocale.EN))
                    .isEmpty();
        }
    }

    @Test
    void businessItemRefNeverAppearsInGeneratedQuestionText() {
        List<ProcessEffortMaterialityEvidenceGap> gaps = identify(
                processIdentified(),
                evidence(absent("secret-item-ref", "ticket"), absent("secret-item-ref", "ticket")),
                notEstablished(),
                ProcessAnalysisLocale.EN);

        assertThat(evidenceGaps(gaps)).extracting(ProcessEvidenceGap::question)
                .allSatisfy(question -> assertThat(question)
                        .doesNotContain("secret-item-ref"));
        assertThat(gaps).extracting(ProcessEffortMaterialityEvidenceGap::kind)
                .containsExactly(
                        ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD,
                        ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM);
    }

    @Test
    void nonblankBusinessItemLabelDoesNotAffectExistenceOrderingDecisionOrKind() {
        List<ProcessEffortMaterialityEvidenceGap> first = identify(
                processIdentified(),
                evidence(absent("item-1", "ticket"), absent("item-1", "ticket")),
                notEstablished(),
                ProcessAnalysisLocale.EN);
        List<ProcessEffortMaterialityEvidenceGap> second = identify(
                processIdentified(),
                evidence(absent("item-1", "invoice"), absent("item-1", "invoice")),
                notEstablished(),
                ProcessAnalysisLocale.EN);

        assertThat(second).hasSameSizeAs(first);
        assertThat(second).extracting(ProcessEffortMaterialityEvidenceGap::kind)
                .containsExactlyElementsOf(first.stream().map(ProcessEffortMaterialityEvidenceGap::kind).toList());
        assertThat(evidenceGaps(second)).extracting(ProcessEvidenceGap::question)
                .containsExactlyElementsOf(evidenceGaps(first).stream().map(ProcessEvidenceGap::question).toList());
        assertThat(evidenceGaps(second)).extracting(ProcessEvidenceGap::decisionAffected)
                .containsExactlyElementsOf(evidenceGaps(first).stream().map(ProcessEvidenceGap::decisionAffected).toList());
    }

    @Test
    void questionTextDoesNotDetermineGapKind() {
        List<ProcessEffortMaterialityEvidenceGap> english = identify(
                processIdentified(),
                evidence(absent("item-1", "ticket"), absent("item-1", "ticket")),
                notEstablished(),
                ProcessAnalysisLocale.EN);
        List<ProcessEffortMaterialityEvidenceGap> spanish = identify(
                processIdentified(),
                evidence(absent("item-1", "ticket"), absent("item-1", "ticket")),
                notEstablished(),
                ProcessAnalysisLocale.ES);

        assertThat(evidenceGaps(spanish)).extracting(ProcessEvidenceGap::question)
                .doesNotContain(evidenceGaps(english).stream().map(ProcessEvidenceGap::question).toArray(String[]::new));
        assertThat(spanish).extracting(ProcessEffortMaterialityEvidenceGap::kind)
                .containsExactlyElementsOf(english.stream().map(ProcessEffortMaterialityEvidenceGap::kind).toList());
    }

    @Test
    void localeChangesQuestionRenderingButNotGapKind() {
        List<ProcessEffortMaterialityEvidenceGap> english = identify(
                processIdentified(),
                evidence(absent("item-1", "ticket"), absent("item-1", "ticket")),
                notEstablished(),
                ProcessAnalysisLocale.EN);
        List<ProcessEffortMaterialityEvidenceGap> italian = identify(
                processIdentified(),
                evidence(absent("item-1", "ticket"), absent("item-1", "ticket")),
                notEstablished(),
                ProcessAnalysisLocale.IT);

        assertThat(evidenceGaps(italian)).extracting(ProcessEvidenceGap::question)
                .containsExactly(
                        "Quale quantità mensile usi come volume di riferimento per questo processo?",
                        "Quanti minuti di lavoro per elemento di business processato usi come valore di riferimento?");
        assertThat(italian).extracting(ProcessEffortMaterialityEvidenceGap::kind)
                .containsExactlyElementsOf(english.stream().map(ProcessEffortMaterialityEvidenceGap::kind).toList());
    }

    @Test
    void rendersDeterministicEnglishQuestions() {
        assertThat(questions(ProcessAnalysisLocale.EN)).containsExactly(
                "What monthly quantity do you use as the reference volume for this process?",
                "How many minutes of effort per processed business item do you use as the reference value?");
    }

    @Test
    void rendersDeterministicSpanishQuestions() {
        assertThat(questions(ProcessAnalysisLocale.ES)).containsExactly(
                "¿Qué cantidad mensual usas como volumen de referencia para este proceso?",
                "¿Cuántos minutos de esfuerzo por ítem de negocio procesado usas como valor de referencia?");
    }

    @Test
    void rendersDeterministicItalianQuestions() {
        assertThat(questions(ProcessAnalysisLocale.IT)).containsExactly(
                "Quale quantità mensile usi come volume di riferimento per questo processo?",
                "Quanti minuti di lavoro per elemento di business processato usi come valore di riferimento?");
    }

    @Test
    void rendersDeterministicNonExactQuestionsForAllLocales() {
        assertThat(questionFor(
                        approximateVolume("4", "item-1", "ticket"),
                        exactEffort("2", "item-1", "ticket"),
                        ProcessAnalysisLocale.EN))
                .isEqualTo("The description provides an approximate monthly volume. What exact monthly quantity do you want to use as the scalar reference value for this calculation?");
        assertThat(questionFor(
                        rangeVolume("4", "5", "item-1", "ticket"),
                        exactEffort("2", "item-1", "ticket"),
                        ProcessAnalysisLocale.ES))
                .isEqualTo("La descripci\u00f3n aporta un rango para el volumen mensual. \u00bfQu\u00e9 cantidad mensual puntual quer\u00e9s usar como valor escalar de referencia para este c\u00e1lculo?");
        assertThat(questionFor(
                        exactVolume("4000", "item-1", "ticket"),
                        approximateEffort("2", "item-1", "ticket"),
                        ProcessAnalysisLocale.IT))
                .isEqualTo("La descrizione fornisce uno sforzo approssimativo per elemento. Quanti minuti per elemento vuoi usare come valore scalare puntuale di riferimento per questo calcolo?");
        assertThat(questionFor(
                        exactVolume("4000", "item-1", "ticket"),
                        rangeEffort("1", "3", "item-1", "ticket"),
                        ProcessAnalysisLocale.IT))
                .isEqualTo("La descrizione fornisce un intervallo di sforzo per elemento. Quanti minuti per elemento vuoi usare come valore scalare puntuale di riferimento per questo calcolo?");
    }

    private List<String> questions(ProcessAnalysisLocale locale) {
        return identify(
                processIdentified(),
                evidence(absent("item-1", "ticket"), absent("item-1", "ticket")),
                notEstablished(),
                locale).stream()
                .map(ProcessEffortMaterialityEvidenceGap::evidenceGap)
                .map(ProcessEvidenceGap::question)
                .toList();
    }

    private String questionFor(
            ProcessEffortEvidenceQuantity volume,
            ProcessEffortEvidenceQuantity effort,
            ProcessAnalysisLocale locale) {
        return identify(
                processIdentified(),
                evidence(volume, effort),
                notEstablished(),
                locale).getFirst().evidenceGap().question();
    }

    private List<ProcessEffortMaterialityEvidenceGap> identify(
            ProcessUnderstanding understanding,
            ProcessEffortEvidence evidence,
            ProcessEffortMaterialityAssessment assessment,
            ProcessAnalysisLocale locale) {
        return identifier.identify(understanding, evidence, assessment, locale);
    }

    private List<ProcessEvidenceGap> evidenceGaps(List<ProcessEffortMaterialityEvidenceGap> gaps) {
        return gaps.stream().map(ProcessEffortMaterialityEvidenceGap::evidenceGap).toList();
    }

    private ProcessEffortMaterialityAssessment notEstablished() {
        return ProcessEffortMaterialityAssessment.notEstablished(ProcessEffortMaterialityThreshold.P06_LAB_POLICY);
    }

    private ProcessEffortMaterialityAssessment establishedNoMaterialJustification() {
        return new ProcessEffortMaterialityAssessment(
                ProcessEffortMaterialityAssessmentStatus.NO_MATERIAL_JUSTIFICATION_IDENTIFIED,
                ProcessEffortMaterialityThreshold.P06_LAB_POLICY,
                Optional.of(new ProcessQuantityProjection(
                        new BigDecimal("8"),
                        new ProcessEffortPerReportingPeriodUnit(
                                ProcessEffortDurationUnit.MINUTE,
                                ProcessReportingPeriodUnit.MONTH))));
    }

    private ProcessEffortEvidence evidence(
            ProcessEffortEvidenceQuantity volume,
            ProcessEffortEvidenceQuantity effort) {
        return new ProcessEffortEvidence(volume, effort);
    }

    private ProcessEffortEvidenceQuantity absent(String businessItemRef, String businessItemLabel) {
        return quantity(ProcessEffortEvidenceQuantityStatus.ABSENT, null, businessItemRef, businessItemLabel, null, null);
    }

    private ProcessEffortEvidenceQuantity exactVolume(String magnitude, String businessItemRef, String businessItemLabel) {
        return quantity(
                ProcessEffortEvidenceQuantityStatus.EXACT,
                magnitude,
                businessItemRef,
                businessItemLabel,
                ProcessReportingPeriodUnit.MONTH,
                null);
    }

    private ProcessEffortEvidenceQuantity exactEffort(String magnitude, String businessItemRef, String businessItemLabel) {
        return quantity(
                ProcessEffortEvidenceQuantityStatus.EXACT,
                magnitude,
                businessItemRef,
                businessItemLabel,
                null,
                ProcessEffortDurationUnit.MINUTE);
    }

    private ProcessEffortEvidenceQuantity approximateVolume(
            String magnitude,
            String businessItemRef,
            String businessItemLabel) {
        return quantity(
                ProcessEffortEvidenceQuantityStatus.APPROXIMATE,
                magnitude,
                businessItemRef,
                businessItemLabel,
                ProcessReportingPeriodUnit.MONTH,
                null);
    }

    private ProcessEffortEvidenceQuantity approximateEffort(
            String magnitude,
            String businessItemRef,
            String businessItemLabel) {
        return quantity(
                ProcessEffortEvidenceQuantityStatus.APPROXIMATE,
                magnitude,
                businessItemRef,
                businessItemLabel,
                null,
                ProcessEffortDurationUnit.MINUTE);
    }

    private ProcessEffortEvidenceQuantity rangeVolume(
            String minMagnitude,
            String maxMagnitude,
            String businessItemRef,
            String businessItemLabel) {
        return rangeQuantity(
                ProcessEffortEvidenceQuantityStatus.RANGE,
                minMagnitude,
                maxMagnitude,
                businessItemRef,
                businessItemLabel,
                ProcessReportingPeriodUnit.MONTH,
                null);
    }

    private ProcessEffortEvidenceQuantity rangeEffort(
            String minMagnitude,
            String maxMagnitude,
            String businessItemRef,
            String businessItemLabel) {
        return rangeQuantity(
                ProcessEffortEvidenceQuantityStatus.RANGE,
                minMagnitude,
                maxMagnitude,
                businessItemRef,
                businessItemLabel,
                null,
                ProcessEffortDurationUnit.MINUTE);
    }

    private ProcessEffortEvidenceQuantity unsupportedVolume(
            String magnitude,
            String businessItemRef,
            String businessItemLabel) {
        return quantity(
                ProcessEffortEvidenceQuantityStatus.UNSUPPORTED_UNIT,
                magnitude,
                businessItemRef,
                businessItemLabel,
                ProcessReportingPeriodUnit.MONTH,
                null);
    }

    private ProcessEffortEvidenceQuantity unsupportedEffort(
            String magnitude,
            String businessItemRef,
            String businessItemLabel) {
        return quantity(
                ProcessEffortEvidenceQuantityStatus.UNSUPPORTED_UNIT,
                magnitude,
                businessItemRef,
                businessItemLabel,
                null,
                ProcessEffortDurationUnit.MINUTE);
    }

    private ProcessEffortEvidenceQuantity volume(
            ProcessEffortEvidenceQuantityStatus status,
            String businessItemRef,
            String businessItemLabel) {
        return switch (status) {
            case EXACT -> exactVolume("4000", businessItemRef, businessItemLabel);
            case APPROXIMATE, UNSUPPORTED_UNIT -> quantity(
                    status,
                    "4000",
                    businessItemRef,
                    businessItemLabel,
                    ProcessReportingPeriodUnit.MONTH,
                    null);
            case RANGE -> rangeQuantity(
                    status,
                    "3000",
                    "5000",
                    businessItemRef,
                    businessItemLabel,
                    ProcessReportingPeriodUnit.MONTH,
                    null);
            case ABSENT -> absent(businessItemRef, businessItemLabel);
        };
    }

    private ProcessEffortEvidenceQuantity effort(
            ProcessEffortEvidenceQuantityStatus status,
            String businessItemRef,
            String businessItemLabel) {
        return switch (status) {
            case EXACT -> exactEffort("2", businessItemRef, businessItemLabel);
            case APPROXIMATE, UNSUPPORTED_UNIT -> quantity(
                    status,
                    "2",
                    businessItemRef,
                    businessItemLabel,
                    null,
                    ProcessEffortDurationUnit.MINUTE);
            case RANGE -> rangeQuantity(
                    status,
                    "1",
                    "3",
                    businessItemRef,
                    businessItemLabel,
                    null,
                    ProcessEffortDurationUnit.MINUTE);
            case ABSENT -> absent(businessItemRef, businessItemLabel);
        };
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

    private ProcessEffortEvidenceQuantity rangeQuantity(
            ProcessEffortEvidenceQuantityStatus status,
            String minMagnitude,
            String maxMagnitude,
            String businessItemRef,
            String businessItemLabel,
            ProcessReportingPeriodUnit reportingPeriod,
            ProcessEffortDurationUnit effortDuration) {
        return new ProcessEffortEvidenceQuantity(
                status,
                null,
                new BigDecimal(minMagnitude),
                new BigDecimal(maxMagnitude),
                businessItemRef,
                businessItemLabel,
                reportingPeriod,
                effortDuration,
                null,
                null);
    }

    private ProcessUnderstanding processIdentified() {
        return understanding(ProcessAnalysisStatus.PROCESS_IDENTIFIED);
    }

    private ProcessUnderstanding understanding(ProcessAnalysisStatus status) {
        if (status != ProcessAnalysisStatus.PROCESS_IDENTIFIED) {
            return new ProcessUnderstanding("Description", status, List.of(), List.of(), List.of(), List.of(), "");
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
