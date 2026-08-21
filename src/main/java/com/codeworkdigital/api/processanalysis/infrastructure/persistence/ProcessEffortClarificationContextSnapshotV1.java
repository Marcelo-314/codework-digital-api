package com.codeworkdigital.api.processanalysis.infrastructure.persistence;

import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContext;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortEvidence;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortEvidenceQuantity;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortEvidenceQuantityStatus;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortMaterialityAssessment;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortMaterialityAssessmentStatus;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortMaterialityEvidenceGap;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortMaterialityEvidenceGapKind;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortMaterialityThreshold;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortSourceKnowledge;
import com.codeworkdigital.api.processanalysis.domain.ProcessAnalysisScope;
import com.codeworkdigital.api.processanalysis.domain.ProcessBusinessItemPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessBusinessItemUnitId;
import com.codeworkdigital.api.processanalysis.domain.ProcessComputableProjection;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerBusinessItemUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceArtifact;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceArtifactId;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceArtifactKind;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceBase;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceGap;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceSource;
import com.codeworkdigital.api.processanalysis.domain.ProcessFactGrounding;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFact;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFactId;
import com.codeworkdigital.api.processanalysis.domain.ProcessQuantityProjection;
import com.codeworkdigital.api.processanalysis.domain.ProcessQuantityUnitExpression;
import com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

record ProcessEffortClarificationContextSnapshotV1(
        EffortEvidenceSnapshot effortEvidence,
        SourceKnowledgeSnapshot sourceKnowledge,
        MaterialityAssessmentSnapshot materialityAssessment,
        List<MaterialityEvidenceGapSnapshot> actionableGaps) {

    static ProcessEffortClarificationContextSnapshotV1 from(ProcessEffortClarificationContext context) {
        Objects.requireNonNull(context, "context");
        return new ProcessEffortClarificationContextSnapshotV1(
                EffortEvidenceSnapshot.from(context.effortEvidence()),
                SourceKnowledgeSnapshot.from(context.sourceKnowledge()),
                MaterialityAssessmentSnapshot.from(context.materialityAssessment()),
                context.actionableGaps().stream()
                        .map(MaterialityEvidenceGapSnapshot::from)
                        .toList());
    }

    ProcessEffortClarificationContext toContext() {
        return new ProcessEffortClarificationContext(
                effortEvidence.toEvidence(),
                sourceKnowledge.toSourceKnowledge(),
                materialityAssessment.toAssessment(),
                actionableGaps.stream()
                        .map(MaterialityEvidenceGapSnapshot::toGap)
                        .toList());
    }

    record EffortEvidenceSnapshot(
            EffortEvidenceQuantitySnapshot volumePerReportingPeriod,
            EffortEvidenceQuantitySnapshot effortPerBusinessItem) {

        static EffortEvidenceSnapshot from(ProcessEffortEvidence evidence) {
            return new EffortEvidenceSnapshot(
                    EffortEvidenceQuantitySnapshot.from(evidence.volumePerReportingPeriod()),
                    EffortEvidenceQuantitySnapshot.from(evidence.effortPerBusinessItem()));
        }

        ProcessEffortEvidence toEvidence() {
            return new ProcessEffortEvidence(
                    volumePerReportingPeriod.toQuantity(),
                    effortPerBusinessItem.toQuantity());
        }
    }

    record EffortEvidenceQuantitySnapshot(
            String status,
            BigDecimal magnitude,
            BigDecimal minMagnitude,
            BigDecimal maxMagnitude,
            String businessItemRef,
            String businessItemLabel,
            String reportingPeriod,
            String effortDuration,
            String evidenceText,
            String note) {

        static EffortEvidenceQuantitySnapshot from(ProcessEffortEvidenceQuantity quantity) {
            return new EffortEvidenceQuantitySnapshot(
                    quantity.status().name(),
                    quantity.magnitude(),
                    quantity.minMagnitude(),
                    quantity.maxMagnitude(),
                    quantity.businessItemRef(),
                    quantity.businessItemLabel(),
                    nameOrNull(quantity.reportingPeriod()),
                    nameOrNull(quantity.effortDuration()),
                    quantity.evidenceText(),
                    quantity.note());
        }

        ProcessEffortEvidenceQuantity toQuantity() {
            return new ProcessEffortEvidenceQuantity(
                    ProcessEffortEvidenceQuantityStatus.valueOf(status),
                    magnitude,
                    minMagnitude,
                    maxMagnitude,
                    businessItemRef,
                    businessItemLabel,
                    enumOrNull(ProcessReportingPeriodUnit.class, reportingPeriod),
                    enumOrNull(ProcessEffortDurationUnit.class, effortDuration),
                    evidenceText,
                    note);
        }
    }

    record SourceKnowledgeSnapshot(
            EvidenceBaseSnapshot evidenceBase,
            List<KnownFactSnapshot> knownFacts) {

        static SourceKnowledgeSnapshot from(ProcessEffortSourceKnowledge sourceKnowledge) {
            return new SourceKnowledgeSnapshot(
                    EvidenceBaseSnapshot.from(sourceKnowledge.evidenceBase()),
                    sourceKnowledge.knownFacts().stream()
                            .map(KnownFactSnapshot::from)
                            .toList());
        }

        ProcessEffortSourceKnowledge toSourceKnowledge() {
            return new ProcessEffortSourceKnowledge(
                    evidenceBase.toEvidenceBase(),
                    knownFacts.stream()
                            .map(KnownFactSnapshot::toKnownFact)
                            .toList());
        }
    }

    record EvidenceBaseSnapshot(List<EvidenceArtifactSnapshot> artifacts) {

        static EvidenceBaseSnapshot from(ProcessEvidenceBase evidenceBase) {
            return new EvidenceBaseSnapshot(evidenceBase.artifacts().stream()
                    .map(EvidenceArtifactSnapshot::from)
                    .toList());
        }

        ProcessEvidenceBase toEvidenceBase() {
            return new ProcessEvidenceBase(artifacts.stream()
                    .map(EvidenceArtifactSnapshot::toArtifact)
                    .toList());
        }
    }

    record EvidenceArtifactSnapshot(String id, String kind, String description) {

        static EvidenceArtifactSnapshot from(ProcessEvidenceArtifact artifact) {
            return new EvidenceArtifactSnapshot(
                    artifact.id().value(),
                    artifact.kind().name(),
                    artifact.description());
        }

        ProcessEvidenceArtifact toArtifact() {
            return new ProcessEvidenceArtifact(
                    new ProcessEvidenceArtifactId(id),
                    ProcessEvidenceArtifactKind.valueOf(kind),
                    description);
        }
    }

    record KnownFactSnapshot(
            String id,
            String statement,
            String grounding,
            ScopeSnapshot scope,
            List<String> premiseFactIds,
            List<String> evidenceArtifactIds,
            QuantityProjectionSnapshot computableProjection) {

        static KnownFactSnapshot from(ProcessKnownFact fact) {
            return new KnownFactSnapshot(
                    fact.id().value(),
                    fact.statement(),
                    fact.grounding().name(),
                    ScopeSnapshot.from(fact.scope()),
                    fact.premiseFactIds().stream().map(ProcessKnownFactId::value).toList(),
                    fact.evidenceArtifactIds().stream().map(ProcessEvidenceArtifactId::value).toList(),
                    fact.computableProjection()
                            .map(KnownFactSnapshot::projection)
                            .orElse(null));
        }

        private static QuantityProjectionSnapshot projection(ProcessComputableProjection projection) {
            if (projection instanceof ProcessQuantityProjection quantityProjection) {
                return QuantityProjectionSnapshot.from(quantityProjection);
            }
            throw new IllegalArgumentException("unsupported computable projection type: "
                    + projection.getClass().getName());
        }

        ProcessKnownFact toKnownFact() {
            return new ProcessKnownFact(
                    new ProcessKnownFactId(id),
                    statement,
                    ProcessFactGrounding.valueOf(grounding),
                    scope.toScope(),
                    premiseFactIds.stream().map(ProcessKnownFactId::new).toList(),
                    evidenceArtifactIds.stream().map(ProcessEvidenceArtifactId::new).toList(),
                    Optional.ofNullable(computableProjection).map(QuantityProjectionSnapshot::toProjection));
        }
    }

    record MaterialityAssessmentSnapshot(
            String status,
            MaterialityThresholdSnapshot threshold,
            QuantityProjectionSnapshot establishedOperationalBurden) {

        static MaterialityAssessmentSnapshot from(ProcessEffortMaterialityAssessment assessment) {
            return new MaterialityAssessmentSnapshot(
                    assessment.status().name(),
                    MaterialityThresholdSnapshot.from(assessment.threshold()),
                    assessment.establishedOperationalBurden()
                            .map(QuantityProjectionSnapshot::from)
                            .orElse(null));
        }

        ProcessEffortMaterialityAssessment toAssessment() {
            return new ProcessEffortMaterialityAssessment(
                    ProcessEffortMaterialityAssessmentStatus.valueOf(status),
                    threshold.toThreshold(),
                    Optional.ofNullable(establishedOperationalBurden)
                            .map(QuantityProjectionSnapshot::toProjection));
        }
    }

    record MaterialityThresholdSnapshot(QuantityProjectionSnapshot projection) {

        static MaterialityThresholdSnapshot from(ProcessEffortMaterialityThreshold threshold) {
            return new MaterialityThresholdSnapshot(QuantityProjectionSnapshot.from(threshold.projection()));
        }

        ProcessEffortMaterialityThreshold toThreshold() {
            return new ProcessEffortMaterialityThreshold(projection.toProjection());
        }
    }

    record MaterialityEvidenceGapSnapshot(
            String kind,
            EvidenceGapSnapshot evidenceGap) {

        static MaterialityEvidenceGapSnapshot from(ProcessEffortMaterialityEvidenceGap gap) {
            return new MaterialityEvidenceGapSnapshot(
                    gap.kind().name(),
                    EvidenceGapSnapshot.from(gap.evidenceGap()));
        }

        ProcessEffortMaterialityEvidenceGap toGap() {
            return new ProcessEffortMaterialityEvidenceGap(
                    ProcessEffortMaterialityEvidenceGapKind.valueOf(kind),
                    evidenceGap.toGap());
        }
    }

    record EvidenceGapSnapshot(
            String question,
            String source,
            String decisionAffected,
            ScopeSnapshot scope) {

        static EvidenceGapSnapshot from(ProcessEvidenceGap gap) {
            return new EvidenceGapSnapshot(
                    gap.question(),
                    gap.source().name(),
                    gap.decisionAffected(),
                    ScopeSnapshot.from(gap.scope()));
        }

        ProcessEvidenceGap toGap() {
            return new ProcessEvidenceGap(
                    question,
                    ProcessEvidenceSource.valueOf(source),
                    decisionAffected,
                    scope.toScope());
        }
    }

    record QuantityProjectionSnapshot(
            BigDecimal magnitude,
            QuantityUnitSnapshot unit) {

        static QuantityProjectionSnapshot from(ProcessQuantityProjection projection) {
            return new QuantityProjectionSnapshot(
                    projection.magnitude(),
                    QuantityUnitSnapshot.from(projection.unit()));
        }

        ProcessQuantityProjection toProjection() {
            return new ProcessQuantityProjection(magnitude, unit.toUnit());
        }
    }

    record QuantityUnitSnapshot(
            String type,
            String businessItemRef,
            String effortDuration,
            String reportingPeriod) {

        private static final String BUSINESS_ITEM_PER_REPORTING_PERIOD =
                "BUSINESS_ITEM_PER_REPORTING_PERIOD";
        private static final String EFFORT_PER_BUSINESS_ITEM =
                "EFFORT_PER_BUSINESS_ITEM";
        private static final String EFFORT_PER_REPORTING_PERIOD =
                "EFFORT_PER_REPORTING_PERIOD";

        static QuantityUnitSnapshot from(ProcessQuantityUnitExpression unit) {
            return switch (unit) {
                case ProcessBusinessItemPerReportingPeriodUnit businessItemUnit -> new QuantityUnitSnapshot(
                        BUSINESS_ITEM_PER_REPORTING_PERIOD,
                        businessItemUnit.businessItemId().value(),
                        null,
                        businessItemUnit.reportingPeriod().name());
                case ProcessEffortPerBusinessItemUnit effortUnit -> new QuantityUnitSnapshot(
                        EFFORT_PER_BUSINESS_ITEM,
                        effortUnit.businessItemId().value(),
                        effortUnit.effortDuration().name(),
                        null);
                case ProcessEffortPerReportingPeriodUnit effortReportingUnit -> new QuantityUnitSnapshot(
                        EFFORT_PER_REPORTING_PERIOD,
                        null,
                        effortReportingUnit.effortDuration().name(),
                        effortReportingUnit.reportingPeriod().name());
            };
        }

        ProcessQuantityUnitExpression toUnit() {
            return switch (type) {
                case BUSINESS_ITEM_PER_REPORTING_PERIOD -> new ProcessBusinessItemPerReportingPeriodUnit(
                        new ProcessBusinessItemUnitId(businessItemRef),
                        ProcessReportingPeriodUnit.valueOf(reportingPeriod));
                case EFFORT_PER_BUSINESS_ITEM -> new ProcessEffortPerBusinessItemUnit(
                        ProcessEffortDurationUnit.valueOf(effortDuration),
                        new ProcessBusinessItemUnitId(businessItemRef));
                case EFFORT_PER_REPORTING_PERIOD -> new ProcessEffortPerReportingPeriodUnit(
                        ProcessEffortDurationUnit.valueOf(effortDuration),
                        ProcessReportingPeriodUnit.valueOf(reportingPeriod));
                default -> throw new IllegalArgumentException("unsupported quantity unit type: " + type);
            };
        }
    }

    record ScopeSnapshot(List<String> operationIds) {

        static ScopeSnapshot from(ProcessAnalysisScope scope) {
            return new ScopeSnapshot(scope.operationIds());
        }

        ProcessAnalysisScope toScope() {
            return ProcessAnalysisScope.operations(operationIds);
        }
    }

    private static String nameOrNull(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static <E extends Enum<E>> E enumOrNull(Class<E> enumType, String value) {
        return value == null ? null : Enum.valueOf(enumType, value);
    }
}
