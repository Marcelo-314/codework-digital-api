package com.codeworkdigital.api.processanalysis.domain;

import java.util.Objects;

public record ProcessEvidenceGap(String question, ProcessEvidenceSource source, String decisionAffected) {

    public ProcessEvidenceGap {
        question = requireNonBlank(question, "question");
        source = Objects.requireNonNull(source, "source");
        decisionAffected = requireNonBlank(decisionAffected, "decisionAffected");
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
