package com.codeworkdigital.api.processanalysis.application;

import com.codeworkdigital.api.processanalysis.domain.ProcessAnalysisScope;
import com.codeworkdigital.api.processanalysis.domain.ProcessBusinessItemPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessBusinessItemUnitId;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerBusinessItemUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceArtifact;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceArtifactId;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceArtifactKind;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceBase;
import com.codeworkdigital.api.processanalysis.domain.ProcessFactGrounding;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFact;
import com.codeworkdigital.api.processanalysis.domain.ProcessQuantityProjection;
import com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ProcessEffortClarificationAnswerMaterializer {

    static final ProcessEvidenceArtifactId VOLUME_CLARIFICATION_ARTIFACT_ID =
            new ProcessEvidenceArtifactId("source-clarification-volume-per-reporting-period");
    static final ProcessEvidenceArtifactId EFFORT_CLARIFICATION_ARTIFACT_ID =
            new ProcessEvidenceArtifactId("source-clarification-effort-per-business-item");

    private static final ProcessEvidenceArtifact VOLUME_CLARIFICATION_ARTIFACT = new ProcessEvidenceArtifact(
            VOLUME_CLARIFICATION_ARTIFACT_ID,
            ProcessEvidenceArtifactKind.SOURCE_MATERIAL,
            "Self-reported clarification answer for P06 volume per reporting period");
    private static final ProcessEvidenceArtifact EFFORT_CLARIFICATION_ARTIFACT = new ProcessEvidenceArtifact(
            EFFORT_CLARIFICATION_ARTIFACT_ID,
            ProcessEvidenceArtifactKind.SOURCE_MATERIAL,
            "Self-reported clarification answer for P06 effort per business item");

    public ProcessEffortClarificationKnowledge materialize(
            List<ProcessEffortMaterialityEvidenceGap> actionableGaps,
            ProcessEffortEvidence effortEvidence,
            List<ProcessEffortMaterialityClarificationAnswer> answers) {
        Objects.requireNonNull(actionableGaps, "actionableGaps");
        Objects.requireNonNull(effortEvidence, "effortEvidence");
        Objects.requireNonNull(answers, "answers");

        Set<ProcessEffortMaterialityEvidenceGapKind> actionableKinds = actionableKinds(actionableGaps);
        Map<ProcessEffortMaterialityEvidenceGapKind, ProcessEffortMaterialityClarificationAnswer> answersByKind =
                answersByKind(answers);

        List<ProcessEvidenceArtifact> artifacts = new ArrayList<>();
        List<ProcessKnownFact> facts = new ArrayList<>();

        materializeVolume(actionableKinds, effortEvidence, answersByKind).ifPresent(knowledge -> {
            artifacts.add(knowledge.artifact());
            facts.add(knowledge.fact());
        });
        materializeEffort(actionableKinds, effortEvidence, answersByKind).ifPresent(knowledge -> {
            artifacts.add(knowledge.artifact());
            facts.add(knowledge.fact());
        });

        return new ProcessEffortClarificationKnowledge(new ProcessEvidenceBase(artifacts), facts);
    }

    private Optional<ClarificationFact> materializeVolume(
            Set<ProcessEffortMaterialityEvidenceGapKind> actionableKinds,
            ProcessEffortEvidence effortEvidence,
            Map<ProcessEffortMaterialityEvidenceGapKind, ProcessEffortMaterialityClarificationAnswer> answersByKind) {
        ProcessEffortMaterialityClarificationAnswer answer =
                answersByKind.get(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD);
        if (answer == null) {
            return Optional.empty();
        }
        requireActionable(actionableKinds, answer.kind());
        ProcessEffortEvidenceQuantity quantity = effortEvidence.volumePerReportingPeriod();
        requireAnswerableQuantity(quantity, answer.kind());

        ProcessQuantityProjection projection = new ProcessQuantityProjection(
                answer.magnitude(),
                new ProcessBusinessItemPerReportingPeriodUnit(
                        new ProcessBusinessItemUnitId(quantity.businessItemRef()),
                        ProcessReportingPeriodUnit.MONTH));
        return Optional.of(new ClarificationFact(
                VOLUME_CLARIFICATION_ARTIFACT,
                new ProcessKnownFact(
                        ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID,
                        volumeStatement(projection, quantity.businessItemLabel()),
                        ProcessFactGrounding.SOURCE_STATED,
                        ProcessAnalysisScope.processWide(),
                        List.of(),
                        List.of(VOLUME_CLARIFICATION_ARTIFACT_ID),
                        Optional.of(projection))));
    }

    private Optional<ClarificationFact> materializeEffort(
            Set<ProcessEffortMaterialityEvidenceGapKind> actionableKinds,
            ProcessEffortEvidence effortEvidence,
            Map<ProcessEffortMaterialityEvidenceGapKind, ProcessEffortMaterialityClarificationAnswer> answersByKind) {
        ProcessEffortMaterialityClarificationAnswer answer =
                answersByKind.get(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM);
        if (answer == null) {
            return Optional.empty();
        }
        requireActionable(actionableKinds, answer.kind());
        ProcessEffortEvidenceQuantity quantity = effortEvidence.effortPerBusinessItem();
        requireAnswerableQuantity(quantity, answer.kind());

        ProcessQuantityProjection projection = new ProcessQuantityProjection(
                answer.magnitude(),
                new ProcessEffortPerBusinessItemUnit(
                        ProcessEffortDurationUnit.MINUTE,
                        new ProcessBusinessItemUnitId(quantity.businessItemRef())));
        return Optional.of(new ClarificationFact(
                EFFORT_CLARIFICATION_ARTIFACT,
                new ProcessKnownFact(
                        ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID,
                        effortStatement(projection, quantity.businessItemLabel()),
                        ProcessFactGrounding.SOURCE_STATED,
                        ProcessAnalysisScope.processWide(),
                        List.of(),
                        List.of(EFFORT_CLARIFICATION_ARTIFACT_ID),
                        Optional.of(projection))));
    }

    private static Set<ProcessEffortMaterialityEvidenceGapKind> actionableKinds(
            List<ProcessEffortMaterialityEvidenceGap> actionableGaps) {
        Set<ProcessEffortMaterialityEvidenceGapKind> kinds = new HashSet<>();
        for (ProcessEffortMaterialityEvidenceGap gap : actionableGaps) {
            kinds.add(Objects.requireNonNull(gap, "actionableGap").kind());
        }
        return kinds;
    }

    private static Map<ProcessEffortMaterialityEvidenceGapKind, ProcessEffortMaterialityClarificationAnswer>
            answersByKind(List<ProcessEffortMaterialityClarificationAnswer> answers) {
        Map<ProcessEffortMaterialityEvidenceGapKind, ProcessEffortMaterialityClarificationAnswer> answersByKind =
                new EnumMap<>(ProcessEffortMaterialityEvidenceGapKind.class);
        for (ProcessEffortMaterialityClarificationAnswer answer : answers) {
            ProcessEffortMaterialityClarificationAnswer existing =
                    answersByKind.putIfAbsent(Objects.requireNonNull(answer, "answer").kind(), answer);
            if (existing != null) {
                throw new IllegalArgumentException("duplicate clarification answer kind: " + answer.kind());
            }
        }
        return answersByKind;
    }

    private static void requireActionable(
            Set<ProcessEffortMaterialityEvidenceGapKind> actionableKinds,
            ProcessEffortMaterialityEvidenceGapKind answerKind) {
        if (!actionableKinds.contains(answerKind)) {
            throw new IllegalArgumentException("clarification answer kind is not currently actionable: " + answerKind);
        }
    }

    private static void requireAnswerableQuantity(
            ProcessEffortEvidenceQuantity quantity,
            ProcessEffortMaterialityEvidenceGapKind kind) {
        if (quantity.status() != ProcessEffortEvidenceQuantityStatus.ABSENT
                || isBlank(quantity.businessItemRef())
                || isBlank(quantity.businessItemLabel())) {
            throw new IllegalArgumentException("clarification answer has no answerable evidence context: " + kind);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String volumeStatement(ProcessQuantityProjection projection, String businessItemLabel) {
        ProcessBusinessItemPerReportingPeriodUnit unit =
                (ProcessBusinessItemPerReportingPeriodUnit) projection.unit();
        return "Stated volume for business item '%s': %s per %s"
                .formatted(
                        businessItemLabel,
                        projection.magnitude().toPlainString(),
                        unit.reportingPeriod().name().toLowerCase());
    }

    private static String effortStatement(ProcessQuantityProjection projection, String businessItemLabel) {
        ProcessEffortPerBusinessItemUnit unit = (ProcessEffortPerBusinessItemUnit) projection.unit();
        return "Stated effort for business item '%s': %s %s per item"
                .formatted(
                        businessItemLabel,
                        projection.magnitude().toPlainString(),
                        unit.effortDuration().name().toLowerCase());
    }

    private record ClarificationFact(
            ProcessEvidenceArtifact artifact,
            ProcessKnownFact fact) {
    }
}
