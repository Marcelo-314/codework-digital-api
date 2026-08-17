package com.codeworkdigital.api.processanalysis.domain;

import java.util.Objects;

public record ProcessKnownFact(String statement) {

    public ProcessKnownFact {
        statement = requireNonBlank(statement, "statement");
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
