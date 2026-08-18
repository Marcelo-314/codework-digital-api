package com.codeworkdigital.api.processanalysis.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Established proposition inside process-analysis knowledge.
 *
 * premiseFactIds identifies the established propositions used as immediate premises. It does not represent a formula,
 * calculation, transformation, rule, proof, or executable specification.
 */
public record ProcessKnownFact(
        ProcessKnownFactId id,
        String statement,
        ProcessFactGrounding grounding,
        ProcessAnalysisScope scope,
        List<ProcessKnownFactId> premiseFactIds,
        List<ProcessEvidenceArtifactId> evidenceArtifactIds) {

    public ProcessKnownFact {
        id = Objects.requireNonNull(id, "id");
        statement = requireNonBlank(statement, "statement");
        grounding = Objects.requireNonNull(grounding, "grounding");
        scope = Objects.requireNonNull(scope, "scope");
        premiseFactIds = List.copyOf(premiseFactIds);
        evidenceArtifactIds = List.copyOf(evidenceArtifactIds);
        validatePremiseFactIds(grounding, premiseFactIds);
        validateEvidenceArtifactIds(grounding, evidenceArtifactIds);
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
