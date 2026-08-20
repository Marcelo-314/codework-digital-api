package com.codeworkdigital.api.processanalysis.application;

import com.codeworkdigital.api.processanalysis.domain.ProcessAnalysisScope;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceGap;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ProcessEffortMaterialityEvidenceGapIdentifier {

    static final String DECISION_AFFECTED = "P06 process effort materiality assessment";

    public List<ProcessEvidenceGap> identify(
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

        List<ProcessEvidenceGap> gaps = new ArrayList<>();
        if (effortEvidence.volumePerReportingPeriod().status() == ProcessEffortEvidenceQuantityStatus.ABSENT) {
            gaps.add(gap(question(QuestionKind.VOLUME, locale)));
        }
        if (effortEvidence.effortPerBusinessItem().status() == ProcessEffortEvidenceQuantityStatus.ABSENT) {
            gaps.add(gap(question(QuestionKind.EFFORT, locale)));
        }
        return List.copyOf(gaps);
    }

    private static ProcessEvidenceGap gap(String question) {
        return new ProcessEvidenceGap(
                question,
                ProcessEvidenceSource.SELF_REPORTED,
                DECISION_AFFECTED,
                ProcessAnalysisScope.processWide());
    }

    private static String question(QuestionKind kind, ProcessAnalysisLocale locale) {
        return switch (locale) {
            case EN -> switch (kind) {
                case VOLUME -> "What monthly quantity do you use as the reference volume for this process?";
                case EFFORT -> "How many minutes of effort per processed business item do you use as the reference value?";
            };
            case ES -> switch (kind) {
                case VOLUME -> "Que cantidad mensual usas como volumen de referencia para este proceso?";
                case EFFORT -> "Cuantos minutos de esfuerzo por item de negocio procesado usas como valor de referencia?";
            };
            case IT -> switch (kind) {
                case VOLUME -> "Quale quantita mensile usi come volume di riferimento per questo processo?";
                case EFFORT -> "Quanti minuti di lavoro per elemento di business processato usi come valore di riferimento?";
            };
        };
    }

    private enum QuestionKind {
        VOLUME,
        EFFORT
    }
}
