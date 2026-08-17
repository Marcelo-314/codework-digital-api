package com.codeworkdigital.api.processanalysis.domain;

import java.util.Objects;

public record ProcessEvidenceGap(
        String question,
        ProcessEvidenceSource source,
        String decisionAffected,
        ProcessAnalysisScope scope) {

    public ProcessEvidenceGap {
        question = requireNonBlank(question, "question");
        source = Objects.requireNonNull(source, "source");
        decisionAffected = requireNonBlank(decisionAffected, "decisionAffected");
        scope = Objects.requireNonNull(scope, "scope");
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
