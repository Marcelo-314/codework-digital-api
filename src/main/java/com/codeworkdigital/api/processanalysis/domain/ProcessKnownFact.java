package com.codeworkdigital.api.processanalysis.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ProcessKnownFact(
        String statement,
        ProcessFactGrounding grounding,
        ProcessAnalysisScope scope,
        List<ProcessEvidenceArtifactId> evidenceArtifactIds) {

    public ProcessKnownFact {
        statement = requireNonBlank(statement, "statement");
        grounding = Objects.requireNonNull(grounding, "grounding");
        scope = Objects.requireNonNull(scope, "scope");
        evidenceArtifactIds = List.copyOf(evidenceArtifactIds);
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
}
