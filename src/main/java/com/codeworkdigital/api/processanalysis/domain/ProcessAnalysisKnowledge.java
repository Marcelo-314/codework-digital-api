package com.codeworkdigital.api.processanalysis.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public record ProcessAnalysisKnowledge(
        List<ProcessKnownFact> knownFacts,
        List<ProcessInference> inferences,
        List<ProcessEvidenceGap> evidenceGaps,
        List<ProcessConstraint> constraints) {

    public ProcessAnalysisKnowledge {
        knownFacts = List.copyOf(knownFacts);
        inferences = List.copyOf(inferences);
        evidenceGaps = List.copyOf(evidenceGaps);
        constraints = List.copyOf(constraints);

        Set<ProcessKnownFactId> knownFactIds = new HashSet<>();
        for (ProcessKnownFact knownFact : knownFacts) {
            ProcessKnownFactId knownFactId = knownFact.id();
            if (!knownFactIds.add(knownFactId)) {
                throw new IllegalArgumentException("known fact id must be unique: " + knownFactId.value());
            }
        }
        validatePremiseReferences(knownFacts, knownFactIds);
        validateAcyclicPremiseRelationships(knownFacts);
    }

    private static void validatePremiseReferences(
            List<ProcessKnownFact> knownFacts,
            Set<ProcessKnownFactId> knownFactIds) {
        for (ProcessKnownFact knownFact : knownFacts) {
            for (ProcessKnownFactId premiseFactId : knownFact.premiseFactIds()) {
                if (!knownFactIds.contains(premiseFactId)) {
                    throw new IllegalArgumentException(
                            "known fact references an unknown premise fact: " + premiseFactId.value());
                }
                if (knownFact.id().equals(premiseFactId)) {
                    throw new IllegalArgumentException(
                            "known fact cannot reference itself as a premise: " + premiseFactId.value());
                }
            }
        }
    }

    private static void validateAcyclicPremiseRelationships(List<ProcessKnownFact> knownFacts) {
        Map<ProcessKnownFactId, ProcessKnownFact> knownFactsById = knownFacts.stream()
                .collect(Collectors.toMap(ProcessKnownFact::id, Function.identity()));
        Set<ProcessKnownFactId> visiting = new HashSet<>();
        Set<ProcessKnownFactId> visited = new HashSet<>();

        for (ProcessKnownFact knownFact : knownFacts) {
            visitPremises(knownFact.id(), knownFactsById, visiting, visited);
        }
    }

    private static void visitPremises(
            ProcessKnownFactId knownFactId,
            Map<ProcessKnownFactId, ProcessKnownFact> knownFactsById,
            Set<ProcessKnownFactId> visiting,
            Set<ProcessKnownFactId> visited) {
        if (visited.contains(knownFactId)) {
            return;
        }
        if (!visiting.add(knownFactId)) {
            throw new IllegalArgumentException("cyclic known-fact premise relationship");
        }

        ProcessKnownFact knownFact = knownFactsById.get(knownFactId);
        for (ProcessKnownFactId premiseFactId : knownFact.premiseFactIds()) {
            visitPremises(premiseFactId, knownFactsById, visiting, visited);
        }

        visiting.remove(knownFactId);
        visited.add(knownFactId);
    }
}
