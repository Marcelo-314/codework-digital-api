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
        List<ProcessConstraint> constraints,
        List<ProcessDeterministicDerivation> deterministicDerivations) {

    public ProcessAnalysisKnowledge {
        knownFacts = List.copyOf(knownFacts);
        inferences = List.copyOf(inferences);
        evidenceGaps = List.copyOf(evidenceGaps);
        constraints = List.copyOf(constraints);
        deterministicDerivations = List.copyOf(deterministicDerivations);

        Set<ProcessKnownFactId> knownFactIds = new HashSet<>();
        for (ProcessKnownFact knownFact : knownFacts) {
            ProcessKnownFactId knownFactId = knownFact.id();
            if (!knownFactIds.add(knownFactId)) {
                throw new IllegalArgumentException("known fact id must be unique: " + knownFactId.value());
            }
        }
        Map<ProcessKnownFactId, ProcessKnownFact> knownFactsById = knownFacts.stream()
                .collect(Collectors.toMap(ProcessKnownFact::id, Function.identity()));
        validatePremiseReferences(knownFacts, knownFactIds);
        validateAcyclicPremiseRelationships(knownFacts);
        validateDeterministicDerivations(deterministicDerivations, knownFactsById);
    }

    public ProcessAnalysisKnowledge(
            List<ProcessKnownFact> knownFacts,
            List<ProcessInference> inferences,
            List<ProcessEvidenceGap> evidenceGaps,
            List<ProcessConstraint> constraints) {
        this(knownFacts, inferences, evidenceGaps, constraints, List.of());
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

    private static void validateDeterministicDerivations(
            List<ProcessDeterministicDerivation> deterministicDerivations,
            Map<ProcessKnownFactId, ProcessKnownFact> knownFactsById) {
        for (ProcessDeterministicDerivation derivation : deterministicDerivations) {
            switch (derivation) {
                case ProcessEffortPerReportingPeriodDerivation effortDerivation ->
                        validateEffortPerReportingPeriodDerivation(effortDerivation, knownFactsById);
            }
        }
    }

    private static void validateEffortPerReportingPeriodDerivation(
            ProcessEffortPerReportingPeriodDerivation derivation,
            Map<ProcessKnownFactId, ProcessKnownFact> knownFactsById) {
        ProcessKnownFact volumeFact =
                requireKnownFact(knownFactsById, derivation.volumeFactId(), "volumeFactId");
        ProcessKnownFact effortFact = requireKnownFact(
                knownFactsById,
                derivation.effortPerBusinessItemFactId(),
                "effortPerBusinessItemFactId");
        ProcessKnownFact resultFact =
                requireKnownFact(knownFactsById, derivation.resultFactId(), "resultFactId");

        if (resultFact.grounding() != ProcessFactGrounding.DETERMINISTICALLY_DERIVED) {
            throw new IllegalArgumentException(
                    "effort per reporting period derivation result fact must be DETERMINISTICALLY_DERIVED");
        }

        Set<ProcessKnownFactId> expectedPremiseFactIds =
                Set.of(derivation.volumeFactId(), derivation.effortPerBusinessItemFactId());
        Set<ProcessKnownFactId> actualPremiseFactIds = new HashSet<>(resultFact.premiseFactIds());
        if (!actualPremiseFactIds.equals(expectedPremiseFactIds)
                || resultFact.premiseFactIds().size() != expectedPremiseFactIds.size()) {
            throw new IllegalArgumentException(
                    "effort per reporting period derivation result premises must match declared operand facts");
        }

        ProcessBusinessItemPerReportingPeriodUnit volumeUnit = requireQuantityUnit(
                volumeFact,
                ProcessBusinessItemPerReportingPeriodUnit.class,
                "volumeFactId");
        ProcessEffortPerBusinessItemUnit effortUnit = requireQuantityUnit(
                effortFact,
                ProcessEffortPerBusinessItemUnit.class,
                "effortPerBusinessItemFactId");
        ProcessEffortPerReportingPeriodUnit resultUnit = requireQuantityUnit(
                resultFact,
                ProcessEffortPerReportingPeriodUnit.class,
                "resultFactId");

        if (!volumeUnit.businessItemId().equals(effortUnit.businessItemId())) {
            throw new IllegalArgumentException(
                    "effort per reporting period derivation business item units must match");
        }
        if (volumeUnit.reportingPeriod() != resultUnit.reportingPeriod()) {
            throw new IllegalArgumentException(
                    "effort per reporting period derivation reporting periods must match");
        }
        if (effortUnit.effortDuration() != resultUnit.effortDuration()) {
            throw new IllegalArgumentException(
                    "effort per reporting period derivation effort durations must match");
        }
    }

    private static ProcessKnownFact requireKnownFact(
            Map<ProcessKnownFactId, ProcessKnownFact> knownFactsById,
            ProcessKnownFactId factId,
            String role) {
        ProcessKnownFact knownFact = knownFactsById.get(factId);
        if (knownFact == null) {
            throw new IllegalArgumentException(
                    "deterministic derivation references an unknown " + role + ": " + factId.value());
        }
        return knownFact;
    }

    private static <T extends ProcessQuantityUnitExpression> T requireQuantityUnit(
            ProcessKnownFact knownFact,
            Class<T> unitType,
            String role) {
        ProcessComputableProjection projection = knownFact.computableProjection()
                .orElseThrow(() -> new IllegalArgumentException(
                        "effort per reporting period derivation " + role + " requires a computable projection"));
        if (!(projection instanceof ProcessQuantityProjection quantityProjection)) {
            throw new IllegalArgumentException(
                    "effort per reporting period derivation " + role + " requires a quantity projection");
        }
        if (!unitType.isInstance(quantityProjection.unit())) {
            throw new IllegalArgumentException("effort per reporting period derivation " + role
                    + " requires unit " + unitType.getSimpleName());
        }
        return unitType.cast(quantityProjection.unit());
    }
}
