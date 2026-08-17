package com.codeworkdigital.api.processanalysis.domain;

import java.util.Objects;

public record ProcessConstraint(String statement, String source, String decisionAffected, ProcessAnalysisScope scope) {

    public ProcessConstraint {
        statement = requireNonBlank(statement, "statement");
        source = requireNonBlank(source, "source");
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
