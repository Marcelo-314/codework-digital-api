package com.codeworkdigital.api.processanalysis.application;

import com.codeworkdigital.api.processanalysis.domain.ProcessAnalysisScope;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceGap;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceSource;
import com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ProcessEffortMaterialityEvidenceGapIdentifier {

    static final String DECISION_AFFECTED = "P06 process effort materiality assessment";

    public List<ProcessEffortMaterialityEvidenceGap> identify(
            ProcessUnderstanding understanding,
            ProcessEffortEvidence effortEvidence,
            ProcessEffortMaterialityAssessment materialityAssessment,
            ProcessAnalysisLocale locale) {
        Objects.requireNonNull(understanding, "understanding");
        Objects.requireNonNull(effortEvidence, "effortEvidence");
        Objects.requireNonNull(materialityAssessment, "materialityAssessment");
        Objects.requireNonNull(locale, "locale");

        if (!understanding.isProcessIdentified()
                || materialityAssessment.status() != ProcessEffortMaterialityAssessmentStatus.NOT_ESTABLISHED) {
            return List.of();
        }

        ProcessEffortEvidenceQuantity volume = effortEvidence.volumePerReportingPeriod();
        ProcessEffortEvidenceQuantity effort = effortEvidence.effortPerBusinessItem();
        boolean volumeAbsent = volume.status() == ProcessEffortEvidenceQuantityStatus.ABSENT;
        boolean effortAbsent = effort.status() == ProcessEffortEvidenceQuantityStatus.ABSENT;

        List<ProcessEffortMaterialityEvidenceGap> gaps = new ArrayList<>();
        if (volumeAbsent
                && effortAbsent
                && answerableAbsentTarget(volume)
                && answerableAbsentTarget(effort)
                && sameBusinessItemRef(volume, effort)) {
            gaps.add(gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, locale));
            gaps.add(gap(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM, locale));
        } else if (volumeAbsent
                && answerableAbsentTarget(volume)
                && admissibleExistingEffort(effort)
                && sameBusinessItemRef(volume, effort)) {
            gaps.add(gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, locale));
        } else if (effortAbsent
                && answerableAbsentTarget(effort)
                && admissibleExistingVolume(volume)
                && sameBusinessItemRef(volume, effort)) {
            gaps.add(gap(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM, locale));
        }
        return List.copyOf(gaps);
    }

    private static boolean admissibleExistingVolume(ProcessEffortEvidenceQuantity quantity) {
        return quantity.status() == ProcessEffortEvidenceQuantityStatus.EXACT
                && quantity.magnitude() != null
                && quantity.magnitude().signum() >= 0
                && quantity.minMagnitude() == null
                && quantity.maxMagnitude() == null
                && quantity.reportingPeriod() == ProcessReportingPeriodUnit.MONTH
                && quantity.effortDuration() == null
                && !isBlank(quantity.businessItemRef())
                && !isBlank(quantity.businessItemLabel());
    }

    private static boolean admissibleExistingEffort(ProcessEffortEvidenceQuantity quantity) {
        return quantity.status() == ProcessEffortEvidenceQuantityStatus.EXACT
                && quantity.magnitude() != null
                && quantity.magnitude().signum() >= 0
                && quantity.minMagnitude() == null
                && quantity.maxMagnitude() == null
                && quantity.reportingPeriod() == null
                && quantity.effortDuration() == ProcessEffortDurationUnit.MINUTE
                && !isBlank(quantity.businessItemRef())
                && !isBlank(quantity.businessItemLabel());
    }

    private static boolean answerableAbsentTarget(ProcessEffortEvidenceQuantity quantity) {
        return quantity.status() == ProcessEffortEvidenceQuantityStatus.ABSENT
                && !isBlank(quantity.businessItemRef())
                && !isBlank(quantity.businessItemLabel());
    }

    private static boolean sameBusinessItemRef(
            ProcessEffortEvidenceQuantity volume,
            ProcessEffortEvidenceQuantity effort) {
        return Objects.equals(volume.businessItemRef(), effort.businessItemRef());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static ProcessEffortMaterialityEvidenceGap gap(
            ProcessEffortMaterialityEvidenceGapKind kind,
            ProcessAnalysisLocale locale) {
        return new ProcessEffortMaterialityEvidenceGap(
                kind,
                new ProcessEvidenceGap(
                        question(kind, locale),
                        ProcessEvidenceSource.SELF_REPORTED,
                        DECISION_AFFECTED,
                        ProcessAnalysisScope.processWide()));
    }

    private static String question(ProcessEffortMaterialityEvidenceGapKind kind, ProcessAnalysisLocale locale) {
        return switch (locale) {
            case EN -> switch (kind) {
                case VOLUME_PER_REPORTING_PERIOD -> "What monthly quantity do you use as the reference volume for this process?";
                case EFFORT_PER_BUSINESS_ITEM -> "How many minutes of effort per processed business item do you use as the reference value?";
            };
            case ES -> switch (kind) {
                case VOLUME_PER_REPORTING_PERIOD -> "¿Qué cantidad mensual usas como volumen de referencia para este proceso?";
                case EFFORT_PER_BUSINESS_ITEM -> "¿Cuántos minutos de esfuerzo por ítem de negocio procesado usas como valor de referencia?";
            };
            case IT -> switch (kind) {
                case VOLUME_PER_REPORTING_PERIOD -> "Quale quantità mensile usi come volume di riferimento per questo processo?";
                case EFFORT_PER_BUSINESS_ITEM -> "Quanti minuti di lavoro per elemento di business processato usi come valore di riferimento?";
            };
        };
    }
}
