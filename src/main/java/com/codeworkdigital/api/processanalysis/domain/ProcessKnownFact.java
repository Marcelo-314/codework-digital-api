package com.codeworkdigital.api.processanalysis.domain;

import java.util.Objects;

public record ProcessKnownFact(String statement, ProcessFactGrounding grounding, ProcessAnalysisScope scope) {

    public ProcessKnownFact {
        statement = requireNonBlank(statement, "statement");
        grounding = Objects.requireNonNull(grounding, "grounding");
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
