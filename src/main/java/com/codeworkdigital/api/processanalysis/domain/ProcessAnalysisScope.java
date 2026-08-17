package com.codeworkdigital.api.processanalysis.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ProcessAnalysisScope(List<String> operationIds) {

    public ProcessAnalysisScope {
        operationIds = List.copyOf(operationIds);
        Set<String> uniqueOperationIds = new HashSet<>();
        for (String operationId : operationIds) {
            requireNonBlank(operationId, "operationId");
            if (!uniqueOperationIds.add(operationId)) {
                throw new IllegalArgumentException("operation IDs must be unique inside the scope");
            }
        }
    }

    public static ProcessAnalysisScope processWide() {
        return new ProcessAnalysisScope(List.of());
    }

    public static ProcessAnalysisScope operation(String operationId) {
        return new ProcessAnalysisScope(List.of(operationId));
    }

    public static ProcessAnalysisScope operations(List<String> operationIds) {
        return new ProcessAnalysisScope(operationIds);
    }

    public boolean isProcessWide() {
        return operationIds.isEmpty();
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
