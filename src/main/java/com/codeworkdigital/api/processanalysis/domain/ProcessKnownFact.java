package com.codeworkdigital.api.processanalysis.domain;

import java.util.Objects;

public record ProcessKnownFact(String statement, ProcessAnalysisScope scope) {

    public ProcessKnownFact {
        statement = requireNonBlank(statement, "statement");
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
