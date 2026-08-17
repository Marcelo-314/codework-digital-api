package com.codeworkdigital.api.processanalysis.domain;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public record ProcessAnalysisStructure(ProcessOperationGraph operationGraph, ProcessAnalysisKnowledge knowledge) {

    public ProcessAnalysisStructure {
        operationGraph = Objects.requireNonNull(operationGraph, "operationGraph");
        knowledge = Objects.requireNonNull(knowledge, "knowledge");

        Set<String> operationIds = new HashSet<>();
        for (ProcessOperation operation : operationGraph.operations()) {
            operationIds.add(operation.id());
        }

        for (ProcessKnownFact knownFact : knowledge.knownFacts()) {
            validateScope(operationIds, knownFact.scope(), "known fact");
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
}
