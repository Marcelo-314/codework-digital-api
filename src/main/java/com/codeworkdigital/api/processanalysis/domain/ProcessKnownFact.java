package com.codeworkdigital.api.processanalysis.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Established proposition inside process-analysis knowledge.
 *
 * premiseFactIds identifies the established propositions used as immediate premises. It does not represent a formula,
 * calculation, transformation, rule, proof, or executable specification.
 *
 * computableProjection is optional machine-usable content projected from the proposition for supported deterministic
 * analysis. A missing projection is a valid domain case for non-computable facts. For SOURCE_STATED facts the statement
 * remains the source-grounded proposition, and any projection is subordinate extraction/normalization whose presence
 * alone does not prove extraction correctness. For DETERMINISTICALLY_DERIVED facts a future verified computation may
 * make the projection the canonical computed result and the statement its human-readable rendering; this type does not
 * require every derived fact to have a projection or verify statement/projection consistency. For
 * EMPIRICALLY_ESTABLISHED facts the projection remains optional under the existing grounding semantics.
 */
public record ProcessKnownFact(
        ProcessKnownFactId id,
        String statement,
        ProcessFactGrounding grounding,
        ProcessAnalysisScope scope,
        List<ProcessKnownFactId> premiseFactIds,
        List<ProcessEvidenceArtifactId> evidenceArtifactIds,
        Optional<ProcessComputableProjection> computableProjection) {

    public ProcessKnownFact {
        id = Objects.requireNonNull(id, "id");
        statement = requireNonBlank(statement, "statement");
        grounding = Objects.requireNonNull(grounding, "grounding");
        scope = Objects.requireNonNull(scope, "scope");
        premiseFactIds = List.copyOf(premiseFactIds);
        evidenceArtifactIds = List.copyOf(evidenceArtifactIds);
        computableProjection = Objects.requireNonNull(computableProjection, "computableProjection");
        validatePremiseFactIds(grounding, premiseFactIds);
        validateEvidenceArtifactIds(grounding, evidenceArtifactIds);
    }

    public ProcessKnownFact(
            ProcessKnownFactId id,
            String statement,
            ProcessFactGrounding grounding,
            ProcessAnalysisScope scope,
            List<ProcessKnownFactId> premiseFactIds,
            List<ProcessEvidenceArtifactId> evidenceArtifactIds) {
        this(id, statement, grounding, scope, premiseFactIds, evidenceArtifactIds, Optional.empty());
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static void validateEvidenceArtifactIds(
            ProcessFactGrounding grounding,
            List<ProcessEvidenceArtifactId> evidenceArtifactIds) {
        Set<ProcessEvidenceArtifactId> uniqueArtifactIds = new HashSet<>();
        for (ProcessEvidenceArtifactId evidenceArtifactId : evidenceArtifactIds) {
            if (!uniqueArtifactIds.add(evidenceArtifactId)) {
                throw new IllegalArgumentException(
                        "known fact evidence artifact id must be unique: " + evidenceArtifactId.value());
            }
        }

        switch (grounding) {
            case SOURCE_STATED, EMPIRICALLY_ESTABLISHED -> {
                if (evidenceArtifactIds.isEmpty()) {
                    throw new IllegalArgumentException(grounding + " known facts require evidence artifact ids");
                }
            }
            case DETERMINISTICALLY_DERIVED -> {
                if (!evidenceArtifactIds.isEmpty()) {
                    throw new IllegalArgumentException(
                            "DETERMINISTICALLY_DERIVED known facts do not accept evidence artifact ids");
                }
            }
        }
    }

    private static void validatePremiseFactIds(
            ProcessFactGrounding grounding,
            List<ProcessKnownFactId> premiseFactIds) {
        Set<ProcessKnownFactId> uniquePremiseFactIds = new HashSet<>();
        for (ProcessKnownFactId premiseFactId : premiseFactIds) {
            if (!uniquePremiseFactIds.add(premiseFactId)) {
                throw new IllegalArgumentException("known fact premise fact id must be unique: "
                        + premiseFactId.value());
            }
        }

        switch (grounding) {
            case SOURCE_STATED, EMPIRICALLY_ESTABLISHED -> {
                if (!premiseFactIds.isEmpty()) {
                    throw new IllegalArgumentException(grounding + " known facts do not accept premise fact ids");
                }
            }
            case DETERMINISTICALLY_DERIVED -> {
                if (premiseFactIds.isEmpty()) {
                    throw new IllegalArgumentException(
                            "DETERMINISTICALLY_DERIVED known facts require premise fact ids");
                }
            }
        }
    }
}
