package com.codeworkdigital.api.processanalysis.domain;

import java.util.Objects;

public record ProcessControlFlow(String sourceOperationId, String targetOperationId, String condition) {

    public ProcessControlFlow(String sourceOperationId, String targetOperationId) {
        this(sourceOperationId, targetOperationId, "");
    }

    public ProcessControlFlow {
        sourceOperationId = requireNonBlank(sourceOperationId, "sourceOperationId");
        targetOperationId = requireNonBlank(targetOperationId, "targetOperationId");
        condition = Objects.requireNonNull(condition, "condition");
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
