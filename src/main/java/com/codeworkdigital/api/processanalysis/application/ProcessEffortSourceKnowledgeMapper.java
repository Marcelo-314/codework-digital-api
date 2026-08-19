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
            Optional<ProcessQuantityProjection> volumeProjection,
            Optional<ProcessQuantityProjection> effortProjection) {
        ProcessEvidenceBase evidenceBase = new ProcessEvidenceBase(List.of(SOURCE_ARTIFACT));
        List<ProcessKnownFact> facts = new ArrayList<>();
        volumeProjection.ifPresent(projection -> facts.add(volumeFact(projection)));
        effortProjection.ifPresent(projection -> facts.add(effortFact(projection)));
        return new ProcessEffortSourceKnowledge(evidenceBase, facts);
    }

    static ProcessEffortSourceKnowledge empty() {
        return new ProcessEffortSourceKnowledge(new ProcessEvidenceBase(List.of()), List.of());
    }

    private static ProcessKnownFact volumeFact(ProcessQuantityProjection projection) {
        return new ProcessKnownFact(
                VOLUME_FACT_ID,
                volumeStatement(projection),
                ProcessFactGrounding.SOURCE_STATED,
                ProcessAnalysisScope.processWide(),
                List.of(),
                List.of(SOURCE_ARTIFACT_ID),
                Optional.of(projection));
    }

    private static ProcessKnownFact effortFact(ProcessQuantityProjection projection) {
        return new ProcessKnownFact(
                EFFORT_FACT_ID,
                effortStatement(projection),
                ProcessFactGrounding.SOURCE_STATED,
                ProcessAnalysisScope.processWide(),
                List.of(),
                List.of(SOURCE_ARTIFACT_ID),
                Optional.of(projection));
    }

    private static String volumeStatement(ProcessQuantityProjection projection) {
        ProcessBusinessItemPerReportingPeriodUnit unit =
                (ProcessBusinessItemPerReportingPeriodUnit) projection.unit();
        return "%s business items per %s are stated for this process"
                .formatted(projection.magnitude().toPlainString(), unit.reportingPeriod().name().toLowerCase());
    }

    private static String effortStatement(ProcessQuantityProjection projection) {
        ProcessEffortPerBusinessItemUnit unit = (ProcessEffortPerBusinessItemUnit) projection.unit();
        return "%s %s per business item is stated for this process"
                .formatted(projection.magnitude().toPlainString(), unit.effortDuration().name().toLowerCase());
    }
}
