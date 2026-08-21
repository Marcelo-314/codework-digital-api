package com.codeworkdigital.api.processanalysis.application;

import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceArtifact;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceArtifactId;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceBase;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFact;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFactId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ProcessEffortEstablishedKnowledgeComposer {

    public ProcessEffortEstablishedKnowledge compose(
            ProcessEffortSourceKnowledge sourceKnowledge,
            ProcessEffortClarificationKnowledge clarificationKnowledge) {
        Objects.requireNonNull(sourceKnowledge, "sourceKnowledge");
        Objects.requireNonNull(clarificationKnowledge, "clarificationKnowledge");

        List<ProcessKnownFact> facts = new ArrayList<>();
        appendCanonicalFact(
                facts,
                sourceKnowledge,
                clarificationKnowledge,
                ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID);
        appendCanonicalFact(
                facts,
                sourceKnowledge,
                clarificationKnowledge,
                ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID);
        appendUnsupportedFacts(facts, sourceKnowledge.knownFacts());
        appendUnsupportedFacts(facts, clarificationKnowledge.knownFacts());

        Map<ProcessEvidenceArtifactId, ProcessEvidenceArtifact> artifactsById = new HashMap<>();
        sourceKnowledge.evidenceBase().artifacts().forEach(artifact -> artifactsById.put(artifact.id(), artifact));
        clarificationKnowledge.evidenceBase().artifacts().forEach(artifact -> artifactsById.put(artifact.id(), artifact));

        List<ProcessEvidenceArtifact> artifacts = new ArrayList<>();
        appendReferencedArtifact(artifacts, artifactsById, facts, ProcessEffortSourceKnowledgeMapper.SOURCE_ARTIFACT_ID);
        appendReferencedArtifact(
                artifacts,
                artifactsById,
                facts,
                ProcessEffortClarificationAnswerMaterializer.VOLUME_CLARIFICATION_ARTIFACT_ID);
        appendReferencedArtifact(
                artifacts,
                artifactsById,
                facts,
                ProcessEffortClarificationAnswerMaterializer.EFFORT_CLARIFICATION_ARTIFACT_ID);

        return new ProcessEffortEstablishedKnowledge(new ProcessEvidenceBase(artifacts), facts);
    }

    private static void appendCanonicalFact(
            List<ProcessKnownFact> facts,
            ProcessEffortSourceKnowledge sourceKnowledge,
            ProcessEffortClarificationKnowledge clarificationKnowledge,
            ProcessKnownFactId factId) {
        ProcessKnownFact sourceFact = findFact(sourceKnowledge.knownFacts(), factId);
        ProcessKnownFact clarificationFact = findFact(clarificationKnowledge.knownFacts(), factId);
        if (countFacts(sourceKnowledge.knownFacts(), factId) > 1
                || countFacts(clarificationKnowledge.knownFacts(), factId) > 1) {
            throw new IllegalArgumentException("duplicate established effort fact id: " + factId.value());
        }
        if (sourceFact != null && clarificationFact != null) {
            throw new IllegalArgumentException("duplicate established effort fact id: " + factId.value());
        }
        if (sourceFact != null) {
            facts.add(sourceFact);
        } else if (clarificationFact != null) {
            facts.add(clarificationFact);
        }
    }

    private static ProcessKnownFact findFact(List<ProcessKnownFact> facts, ProcessKnownFactId factId) {
        return facts.stream()
                .filter(fact -> fact.id().equals(factId))
                .findFirst()
                .orElse(null);
    }

    private static long countFacts(List<ProcessKnownFact> facts, ProcessKnownFactId factId) {
        return facts.stream()
                .filter(fact -> fact.id().equals(factId))
                .count();
    }

    private static void appendUnsupportedFacts(List<ProcessKnownFact> establishedFacts, List<ProcessKnownFact> facts) {
        facts.stream()
                .filter(fact -> !fact.id().equals(ProcessEffortSourceKnowledgeMapper.VOLUME_FACT_ID))
                .filter(fact -> !fact.id().equals(ProcessEffortSourceKnowledgeMapper.EFFORT_FACT_ID))
                .forEach(establishedFacts::add);
    }

    private static void appendReferencedArtifact(
            List<ProcessEvidenceArtifact> artifacts,
            Map<ProcessEvidenceArtifactId, ProcessEvidenceArtifact> artifactsById,
            List<ProcessKnownFact> facts,
            ProcessEvidenceArtifactId artifactId) {
        boolean referenced = facts.stream()
                .flatMap(fact -> fact.evidenceArtifactIds().stream())
                .anyMatch(artifactId::equals);
        if (referenced) {
            ProcessEvidenceArtifact artifact = artifactsById.get(artifactId);
            if (artifact != null) {
                artifacts.add(artifact);
            }
        }
    }
}
