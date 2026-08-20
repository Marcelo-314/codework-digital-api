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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProcessEffortMaterialityEvidenceGapIdentifierTest {

    private final ProcessEffortMaterialityEvidenceGapIdentifier identifier =
            new ProcessEffortMaterialityEvidenceGapIdentifier();

    @Test
    void absentVolumeOnlyProducesOneVolumeGap() {
        List<ProcessEvidenceGap> gaps = identify(
                processIdentified(),
                evidence(absent("volume-item", "ticket"), exactEffort("2", "effort-item", "ticket")),
                notEstablished(),
                ProcessAnalysisLocale.EN);

        assertThat(gaps).singleElement().satisfies(gap -> {
            assertThat(gap.question())
                    .isEqualTo("What monthly quantity do you use as the reference volume for this process?");
            assertThat(gap.source()).isEqualTo(ProcessEvidenceSource.SELF_REPORTED);
            assertThat(gap.scope()).isEqualTo(ProcessAnalysisScope.processWide());
        });
    }

    @Test
    void absentEffortOnlyProducesOneEffortGap() {
        List<ProcessEvidenceGap> gaps = identify(
                processIdentified(),
                evidence(exactVolume("4000", "volume-item", "ticket"), absent("effort-item", "ticket")),
                notEstablished(),
                ProcessAnalysisLocale.EN);

        assertThat(gaps).singleElement().satisfies(gap -> assertThat(gap.question())
                .isEqualTo("How many minutes of effort per processed business item do you use as the reference value?"));
    }

    @Test
    void bothAbsentProduceVolumeThenEffortGaps() {
        List<ProcessEvidenceGap> gaps = identify(
                processIdentified(),
                evidence(absent("volume-item", "ticket"), absent("effort-item", "ticket")),
                notEstablished(),
                ProcessAnalysisLocale.EN);

        assertThat(gaps).hasSize(2);
        assertThat(gaps).extracting(ProcessEvidenceGap::question)
                .containsExactly(
                        "What monthly quantity do you use as the reference volume for this process?",
                        "How many minutes of effort per processed business item do you use as the reference value?");
    }

    @Test
    void generatedGapsUseSelfReportedProcessWideScopeAndStableDecisionAffected() {
        List<ProcessEvidenceGap> gaps = identify(
                processIdentified(),
                evidence(absent("volume-item", "ticket"), absent("effort-item", "ticket")),
                notEstablished(),
                ProcessAnalysisLocale.EN);

        assertThat(gaps).allSatisfy(gap -> {
            assertThat(gap.source()).isEqualTo(ProcessEvidenceSource.SELF_REPORTED);
            assertThat(gap.scope()).isEqualTo(ProcessAnalysisScope.processWide());
            assertThat(gap.decisionAffected())
                    .isEqualTo(ProcessEffortMaterialityEvidenceGapIdentifier.DECISION_AFFECTED);
            assertThat(gap.decisionAffected()).doesNotContain("2400", "40 hours", "INTERVENTION_JUSTIFIED");
        });
        assertThat(gaps).extracting(ProcessEvidenceGap::decisionAffected).containsOnly(
                ProcessEffortMaterialityEvidenceGapIdentifier.DECISION_AFFECTED);
    }

    @Test
    void establishedMaterialityProducesNoGapsEvenWhenEvidenceIsAbsent() {
        List<ProcessEvidenceGap> gaps = identify(
                processIdentified(),
                evidence(absent("volume-item", "ticket"), absent("effort-item", "ticket")),
                establishedNoMaterialJustification(),
                ProcessAnalysisLocale.EN);

        assertThat(gaps).isEmpty();
    }

    @Test
    void nonProcessStatusesProduceNoP06MaterialityGaps() {
        for (ProcessAnalysisStatus status : List.of(
                ProcessAnalysisStatus.OUT_OF_SCOPE,
                ProcessAnalysisStatus.INSUFFICIENT_INFORMATION)) {
            List<ProcessEvidenceGap> gaps = identify(
                    understanding(status),
                    evidence(absent("volume-item", "ticket"), absent("effort-item", "ticket")),
                    notEstablished(),
                    ProcessAnalysisLocale.EN);

            assertThat(gaps).isEmpty();
        }
    }

    @Test
    void unsupportedNonAbsentStatusesProduceNoGapsInThisIncrement() {
        for (ProcessEffortEvidenceQuantityStatus status : List.of(
                ProcessEffortEvidenceQuantityStatus.APPROXIMATE,
                ProcessEffortEvidenceQuantityStatus.RANGE,
                ProcessEffortEvidenceQuantityStatus.UNSUPPORTED_UNIT)) {
            List<ProcessEvidenceGap> gaps = identify(
                    processIdentified(),
                    evidence(quantity(status, "4000", "volume-item", "ticket", ProcessReportingPeriodUnit.MONTH, null),
                            quantity(status, "2", "effort-item", "ticket", null, ProcessEffortDurationUnit.MINUTE)),
                    notEstablished(),
                    ProcessAnalysisLocale.EN);

            assertThat(gaps).isEmpty();
        }
    }

    @Test
    void missingLabelAloneProducesNoGapInThisIncrement() {
        List<ProcessEvidenceGap> gaps = identify(
                processIdentified(),
                evidence(exactVolume("4000", "volume-item", " "), exactEffort("2", "effort-item", " ")),
                notEstablished(),
                ProcessAnalysisLocale.EN);

        assertThat(gaps).isEmpty();
    }

    @Test
    void mismatchedBusinessItemRefsAloneProduceNoGapInThisIncrement() {
        List<ProcessEvidenceGap> gaps = identify(
                processIdentified(),
                evidence(exactVolume("4000", "volume-item", "ticket"), exactEffort("2", "effort-item", "invoice")),
                notEstablished(),
                ProcessAnalysisLocale.EN);

        assertThat(gaps).isEmpty();
    }

    @Test
    void businessItemRefNeverAppearsInGeneratedQuestionText() {
        List<ProcessEvidenceGap> gaps = identify(
                processIdentified(),
                evidence(absent("secret-volume-ref", "ticket"), absent("secret-effort-ref", "ticket")),
                notEstablished(),
                ProcessAnalysisLocale.EN);

        assertThat(gaps).extracting(ProcessEvidenceGap::question)
                .allSatisfy(question -> assertThat(question)
                        .doesNotContain("secret-volume-ref", "secret-effort-ref"));
    }

    @Test
    void nonblankBusinessItemLabelDoesNotAffectExistenceOrderingOrDecision() {
        List<ProcessEvidenceGap> first = identify(
                processIdentified(),
                evidence(absent("volume-item", "ticket"), absent("effort-item", "ticket")),
                notEstablished(),
                ProcessAnalysisLocale.EN);
        List<ProcessEvidenceGap> second = identify(
                processIdentified(),
                evidence(absent("volume-item", "invoice"), absent("effort-item", "invoice")),
                notEstablished(),
                ProcessAnalysisLocale.EN);

        assertThat(second).hasSameSizeAs(first);
        assertThat(second).extracting(ProcessEvidenceGap::decisionAffected)
                .containsExactlyElementsOf(first.stream().map(ProcessEvidenceGap::decisionAffected).toList());
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
                "Que cantidad mensual usas como volumen de referencia para este proceso?",
                "Cuantos minutos de esfuerzo por item de negocio procesado usas como valor de referencia?");
    }

    @Test
    void rendersDeterministicItalianQuestions() {
        assertThat(questions(ProcessAnalysisLocale.IT)).containsExactly(
                "Quale quantita mensile usi come volume di riferimento per questo processo?",
                "Quanti minuti di lavoro per elemento di business processato usi come valore di riferimento?");
    }

    private List<String> questions(ProcessAnalysisLocale locale) {
        return identify(
                processIdentified(),
                evidence(absent("volume-item", "ticket"), absent("effort-item", "ticket")),
                notEstablished(),
                locale).stream()
                .map(ProcessEvidenceGap::question)
                .toList();
    }

    private List<ProcessEvidenceGap> identify(
            ProcessUnderstanding understanding,
            ProcessEffortEvidence evidence,
            ProcessEffortMaterialityAssessment assessment,
            ProcessAnalysisLocale locale) {
        return identifier.identify(understanding, evidence, assessment, locale);
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
