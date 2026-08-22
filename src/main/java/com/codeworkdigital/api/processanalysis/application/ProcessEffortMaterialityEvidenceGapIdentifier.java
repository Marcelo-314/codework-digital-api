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
        boolean volumeEstablished = admissibleExistingVolume(volume);
        boolean effortEstablished = admissibleExistingEffort(effort);
        boolean volumeAnswerable = answerableVolumeTarget(volume);
        boolean effortAnswerable = answerableEffortTarget(effort);

        if (!sameBusinessItemRef(volume, effort)
                || !(volumeEstablished || volumeAnswerable)
                || !(effortEstablished || effortAnswerable)
                || !(volumeAnswerable || effortAnswerable)) {
            return List.of();
        }

        List<ProcessEffortMaterialityEvidenceGap> gaps = new ArrayList<>();
        if (volumeAnswerable) {
            gaps.add(gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, volume.status(), locale));
        }
        if (effortAnswerable) {
            gaps.add(gap(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM, effort.status(), locale));
        }
        return List.copyOf(gaps);
    }

    private static boolean admissibleExistingVolume(ProcessEffortEvidenceQuantity quantity) {
        return quantity != null
                && quantity.status() == ProcessEffortEvidenceQuantityStatus.EXACT
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
        return quantity != null
                && quantity.status() == ProcessEffortEvidenceQuantityStatus.EXACT
                && quantity.magnitude() != null
                && quantity.magnitude().signum() >= 0
                && quantity.minMagnitude() == null
                && quantity.maxMagnitude() == null
                && quantity.reportingPeriod() == null
                && quantity.effortDuration() == ProcessEffortDurationUnit.MINUTE
                && !isBlank(quantity.businessItemRef())
                && !isBlank(quantity.businessItemLabel());
    }

    private static boolean answerableVolumeTarget(ProcessEffortEvidenceQuantity quantity) {
        if (quantity == null) {
            return false;
        }
        return switch (quantity.status()) {
            case ABSENT -> hasBusinessItemContext(quantity);
            case APPROXIMATE -> answerableApproximateTarget(quantity)
                    && quantity.reportingPeriod() == ProcessReportingPeriodUnit.MONTH
                    && quantity.effortDuration() == null;
            case RANGE -> answerableRangeTarget(quantity)
                    && quantity.reportingPeriod() == ProcessReportingPeriodUnit.MONTH
                    && quantity.effortDuration() == null;
            case EXACT, UNSUPPORTED_UNIT -> false;
        };
    }

    private static boolean answerableEffortTarget(ProcessEffortEvidenceQuantity quantity) {
        if (quantity == null) {
            return false;
        }
        return switch (quantity.status()) {
            case ABSENT -> hasBusinessItemContext(quantity);
            case APPROXIMATE -> answerableApproximateTarget(quantity)
                    && quantity.reportingPeriod() == null
                    && quantity.effortDuration() == ProcessEffortDurationUnit.MINUTE;
            case RANGE -> answerableRangeTarget(quantity)
                    && quantity.reportingPeriod() == null
                    && quantity.effortDuration() == ProcessEffortDurationUnit.MINUTE;
            case EXACT, UNSUPPORTED_UNIT -> false;
        };
    }

    private static boolean answerableApproximateTarget(ProcessEffortEvidenceQuantity quantity) {
        return hasBusinessItemContext(quantity)
                && quantity.magnitude() != null
                && quantity.magnitude().signum() >= 0
                && quantity.minMagnitude() == null
                && quantity.maxMagnitude() == null;
    }

    private static boolean answerableRangeTarget(ProcessEffortEvidenceQuantity quantity) {
        return hasBusinessItemContext(quantity)
                && quantity.magnitude() == null
                && quantity.minMagnitude() != null
                && quantity.maxMagnitude() != null
                && quantity.minMagnitude().signum() >= 0
                && quantity.maxMagnitude().signum() >= 0
                && quantity.minMagnitude().compareTo(quantity.maxMagnitude()) <= 0;
    }

    private static boolean hasBusinessItemContext(ProcessEffortEvidenceQuantity quantity) {
        return quantity != null
                && !isBlank(quantity.businessItemRef())
                && !isBlank(quantity.businessItemLabel());
    }

    private static boolean sameBusinessItemRef(
            ProcessEffortEvidenceQuantity volume,
            ProcessEffortEvidenceQuantity effort) {
        return volume != null
                && effort != null
                && !isBlank(volume.businessItemRef())
                && Objects.equals(volume.businessItemRef(), effort.businessItemRef());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static ProcessEffortMaterialityEvidenceGap gap(
            ProcessEffortMaterialityEvidenceGapKind kind,
            ProcessEffortEvidenceQuantityStatus status,
            ProcessAnalysisLocale locale) {
        return new ProcessEffortMaterialityEvidenceGap(
                kind,
                new ProcessEvidenceGap(
                        question(kind, status, locale),
                        ProcessEvidenceSource.SELF_REPORTED,
                        DECISION_AFFECTED,
                        ProcessAnalysisScope.processWide()));
    }

    private static String question(
            ProcessEffortMaterialityEvidenceGapKind kind,
            ProcessEffortEvidenceQuantityStatus status,
            ProcessAnalysisLocale locale) {
        return switch (locale) {
            case EN -> englishQuestion(kind, status);
            case ES -> spanishQuestion(kind, status);
            case IT -> italianQuestion(kind, status);
        };
    }

    private static String englishQuestion(
            ProcessEffortMaterialityEvidenceGapKind kind,
            ProcessEffortEvidenceQuantityStatus status) {
        return switch (kind) {
            case VOLUME_PER_REPORTING_PERIOD -> switch (status) {
                case APPROXIMATE -> "The description provides an approximate monthly volume. What exact monthly quantity do you want to use as the scalar reference value for this calculation?";
                case RANGE -> "The description provides a range for monthly volume. What exact monthly quantity do you want to use as the scalar reference value for this calculation?";
                case ABSENT -> "What monthly quantity do you use as the reference volume for this process?";
                case EXACT, UNSUPPORTED_UNIT -> throw new IllegalArgumentException("unsupported volume question status: " + status);
            };
            case EFFORT_PER_BUSINESS_ITEM -> switch (status) {
                case APPROXIMATE -> "The description provides an approximate effort per item. How many minutes per item do you want to use as the exact scalar reference value for this calculation?";
                case RANGE -> "The description provides a range for effort per item. How many minutes per item do you want to use as the exact scalar reference value for this calculation?";
                case ABSENT -> "How many minutes of effort per processed business item do you use as the reference value?";
                case EXACT, UNSUPPORTED_UNIT -> throw new IllegalArgumentException("unsupported effort question status: " + status);
            };
        };
    }

    private static String spanishQuestion(
            ProcessEffortMaterialityEvidenceGapKind kind,
            ProcessEffortEvidenceQuantityStatus status) {
        return switch (kind) {
            case VOLUME_PER_REPORTING_PERIOD -> switch (status) {
                case APPROXIMATE -> "La descripci\u00f3n aporta un volumen mensual aproximado. \u00bfQu\u00e9 cantidad mensual puntual quer\u00e9s usar como valor escalar de referencia para este c\u00e1lculo?";
                case RANGE -> "La descripci\u00f3n aporta un rango para el volumen mensual. \u00bfQu\u00e9 cantidad mensual puntual quer\u00e9s usar como valor escalar de referencia para este c\u00e1lculo?";
                case ABSENT -> "\u00bfQu\u00e9 cantidad mensual usas como volumen de referencia para este proceso?";
                case EXACT, UNSUPPORTED_UNIT -> throw new IllegalArgumentException("unsupported volume question status: " + status);
            };
            case EFFORT_PER_BUSINESS_ITEM -> switch (status) {
                case APPROXIMATE -> "La descripci\u00f3n aporta un esfuerzo aproximado por \u00edtem. \u00bfCu\u00e1ntos minutos por \u00edtem quer\u00e9s usar como valor escalar de referencia para este c\u00e1lculo?";
                case RANGE -> "La descripci\u00f3n aporta un rango de esfuerzo por \u00edtem. \u00bfCu\u00e1ntos minutos por \u00edtem quer\u00e9s usar como valor escalar de referencia para este c\u00e1lculo?";
                case ABSENT -> "\u00bfCu\u00e1ntos minutos de esfuerzo por \u00edtem de negocio procesado usas como valor de referencia?";
                case EXACT, UNSUPPORTED_UNIT -> throw new IllegalArgumentException("unsupported effort question status: " + status);
            };
        };
    }

    private static String italianQuestion(
            ProcessEffortMaterialityEvidenceGapKind kind,
            ProcessEffortEvidenceQuantityStatus status) {
        return switch (kind) {
            case VOLUME_PER_REPORTING_PERIOD -> switch (status) {
                case APPROXIMATE -> "La descrizione fornisce un volume mensile approssimativo. Quale quantit\u00e0 mensile puntuale vuoi usare come valore scalare di riferimento per questo calcolo?";
                case RANGE -> "La descrizione fornisce un intervallo per il volume mensile. Quale quantit\u00e0 mensile puntuale vuoi usare come valore scalare di riferimento per questo calcolo?";
                case ABSENT -> "Quale quantit\u00e0 mensile usi come volume di riferimento per questo processo?";
                case EXACT, UNSUPPORTED_UNIT -> throw new IllegalArgumentException("unsupported volume question status: " + status);
            };
            case EFFORT_PER_BUSINESS_ITEM -> switch (status) {
                case APPROXIMATE -> "La descrizione fornisce uno sforzo approssimativo per elemento. Quanti minuti per elemento vuoi usare come valore scalare puntuale di riferimento per questo calcolo?";
                case RANGE -> "La descrizione fornisce un intervallo di sforzo per elemento. Quanti minuti per elemento vuoi usare come valore scalare puntuale di riferimento per questo calcolo?";
                case ABSENT -> "Quanti minuti di lavoro per elemento di business processato usi come valore di riferimento?";
                case EXACT, UNSUPPORTED_UNIT -> throw new IllegalArgumentException("unsupported effort question status: " + status);
            };
        };
    }
}
