package com.codeworkdigital.api.processanalysis.application;

import com.codeworkdigital.api.processanalysis.domain.ProcessAnalysisScope;
import com.codeworkdigital.api.processanalysis.domain.ProcessBusinessItemPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerBusinessItemUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceArtifact;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceArtifactId;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceArtifactKind;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceBase;
import com.codeworkdigital.api.processanalysis.domain.ProcessFactGrounding;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFact;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFactId;
import com.codeworkdigital.api.processanalysis.domain.ProcessQuantityProjection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class ProcessEffortSourceKnowledgeMapper {

    static final ProcessEvidenceArtifactId SOURCE_ARTIFACT_ID =
            new ProcessEvidenceArtifactId("source-process-description");
    static final ProcessKnownFactId VOLUME_FACT_ID =
            new ProcessKnownFactId("fact-volume-per-reporting-period");
    static final ProcessKnownFactId EFFORT_FACT_ID =
            new ProcessKnownFactId("fact-effort-per-business-item");

    private static final ProcessEvidenceArtifact SOURCE_ARTIFACT = new ProcessEvidenceArtifact(
            SOURCE_ARTIFACT_ID,
            ProcessEvidenceArtifactKind.SOURCE_MATERIAL,
            "Process description submitted for this analysis");

    private ProcessEffortSourceKnowledgeMapper() {
    }

    static ProcessEffortSourceKnowledge map(
            ProcessEffortEvidence effortEvidence,
            Optional<ProcessQuantityProjection> volumeProjection,
            Optional<ProcessQuantityProjection> effortProjection) {
        ProcessEvidenceBase evidenceBase = new ProcessEvidenceBase(List.of(SOURCE_ARTIFACT));
        List<ProcessKnownFact> facts = new ArrayList<>();
        volumeProjection.ifPresent(projection -> normalizedLabel(
                effortEvidence.volumePerReportingPeriod()).ifPresent(label -> facts.add(volumeFact(projection, label))));
        effortProjection.ifPresent(projection -> normalizedLabel(
                effortEvidence.effortPerBusinessItem()).ifPresent(label -> facts.add(effortFact(projection, label))));
        return new ProcessEffortSourceKnowledge(evidenceBase, facts);
    }

    static ProcessEffortSourceKnowledge empty() {
        return new ProcessEffortSourceKnowledge(new ProcessEvidenceBase(List.of()), List.of());
    }

    private static ProcessKnownFact volumeFact(ProcessQuantityProjection projection, String businessItemLabel) {
        return new ProcessKnownFact(
                VOLUME_FACT_ID,
                volumeStatement(projection, businessItemLabel),
                ProcessFactGrounding.SOURCE_STATED,
                ProcessAnalysisScope.processWide(),
                List.of(),
                List.of(SOURCE_ARTIFACT_ID),
                Optional.of(projection));
    }

    private static ProcessKnownFact effortFact(ProcessQuantityProjection projection, String businessItemLabel) {
        return new ProcessKnownFact(
                EFFORT_FACT_ID,
                effortStatement(projection, businessItemLabel),
                ProcessFactGrounding.SOURCE_STATED,
                ProcessAnalysisScope.processWide(),
                List.of(),
                List.of(SOURCE_ARTIFACT_ID),
                Optional.of(projection));
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

    private static Optional<String> normalizedLabel(ProcessEffortEvidenceQuantity quantity) {
        if (quantity == null || quantity.businessItemLabel() == null) {
            return Optional.empty();
        }
        String label = quantity.businessItemLabel().strip();
        if (label.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(label);
    }
}
