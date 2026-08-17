package com.codeworkdigital.api.processanalysis.domain;

import java.util.Objects;

public record ProcessInference(String statement, ProcessAnalysisScope scope) {

    public ProcessInference {
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
