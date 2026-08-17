package com.codeworkdigital.api.processanalysis.domain;

import java.util.Objects;

public record ProcessInformationFlow(String sourceOperationId, String targetOperationId, String information) {

    public ProcessInformationFlow {
        sourceOperationId = requireNonBlank(sourceOperationId, "sourceOperationId");
        targetOperationId = requireNonBlank(targetOperationId, "targetOperationId");
        information = requireNonBlank(information, "information");
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
