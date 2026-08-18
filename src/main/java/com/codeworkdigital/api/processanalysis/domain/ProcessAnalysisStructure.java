package com.codeworkdigital.api.processanalysis.domain;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public record ProcessAnalysisStructure(
        ProcessOperationGraph operationGraph,
        ProcessEvidenceBase evidenceBase,
        ProcessAnalysisKnowledge knowledge) {

    public ProcessAnalysisStructure {
        operationGraph = Objects.requireNonNull(operationGraph, "operationGraph");
        evidenceBase = Objects.requireNonNull(evidenceBase, "evidenceBase");
        knowledge = Objects.requireNonNull(knowledge, "knowledge");

        Set<String> operationIds = new HashSet<>();
        for (ProcessOperation operation : operationGraph.operations()) {
            operationIds.add(operation.id());
        }

        for (ProcessKnownFact knownFact : knowledge.knownFacts()) {
            validateScope(operationIds, knownFact.scope(), "known fact");
            validateEvidenceReferences(evidenceBase, knownFact);
        }
        for (ProcessInference inference : knowledge.inferences()) {
            validateScope(operationIds, inference.scope(), "inference");
        }
        for (ProcessEvidenceGap evidenceGap : knowledge.evidenceGaps()) {
            validateScope(operationIds, evidenceGap.scope(), "evidence gap");
        }
        for (ProcessConstraint constraint : knowledge.constraints()) {
            validateScope(operationIds, constraint.scope(), "constraint");
        }
    }

    private static void validateScope(Set<String> operationIds, ProcessAnalysisScope scope, String owner) {
        for (String operationId : scope.operationIds()) {
            if (!operationIds.contains(operationId)) {
                throw new IllegalArgumentException(owner + " scope references an unknown operation: " + operationId);
            }
        }
    }

    private static void validateEvidenceReferences(ProcessEvidenceBase evidenceBase, ProcessKnownFact knownFact) {
        ProcessEvidenceArtifactKind requiredKind = requiredEvidenceKind(knownFact.grounding());
        for (ProcessEvidenceArtifactId artifactId : knownFact.evidenceArtifactIds()) {
            ProcessEvidenceArtifact artifact = findArtifact(evidenceBase, artifactId);
            if (artifact == null) {
                throw new IllegalArgumentException(
                        "known fact references an unknown evidence artifact: " + artifactId.value());
            }
            if (artifact.kind() != requiredKind) {
                throw new IllegalArgumentException("known fact grounding " + knownFact.grounding()
                        + " is incompatible with evidence artifact " + artifactId.value()
                        + " kind " + artifact.kind());
            }
        }
    }

    private static ProcessEvidenceArtifactKind requiredEvidenceKind(ProcessFactGrounding grounding) {
        return switch (grounding) {
            case SOURCE_STATED -> ProcessEvidenceArtifactKind.SOURCE_MATERIAL;
            case EMPIRICALLY_ESTABLISHED -> ProcessEvidenceArtifactKind.EMPIRICAL_RESULT;
            case DETERMINISTICALLY_DERIVED -> null;
        };
    }

    private static ProcessEvidenceArtifact findArtifact(
            ProcessEvidenceBase evidenceBase,
            ProcessEvidenceArtifactId artifactId) {
        for (ProcessEvidenceArtifact artifact : evidenceBase.artifacts()) {
            if (artifact.id().equals(artifactId)) {
                return artifact;
            }
        }
        return null;
    }
}
