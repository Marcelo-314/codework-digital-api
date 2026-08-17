package com.codeworkdigital.api.processanalysis.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record ProcessOperationGraph(
        List<ProcessOperation> operations,
        List<ProcessInformationFlow> informationFlows,
        List<ProcessControlFlow> controlFlows) {

    public ProcessOperationGraph {
        operations = List.copyOf(operations);
        informationFlows = List.copyOf(informationFlows);
        controlFlows = List.copyOf(controlFlows);

        if (operations.isEmpty()) {
            throw new IllegalArgumentException("operations must contain at least one item");
        }

        Set<String> operationIds = new HashSet<>();
        for (ProcessOperation operation : operations) {
            if (!operationIds.add(operation.id())) {
                throw new IllegalArgumentException("operation IDs must be unique");
            }
        }

        for (ProcessInformationFlow flow : informationFlows) {
            validateOperationReference(operationIds, flow.sourceOperationId(), "information flow sourceOperationId");
            validateOperationReference(operationIds, flow.targetOperationId(), "information flow targetOperationId");
        }

        for (ProcessControlFlow flow : controlFlows) {
            validateOperationReference(operationIds, flow.sourceOperationId(), "control flow sourceOperationId");
            validateOperationReference(operationIds, flow.targetOperationId(), "control flow targetOperationId");
        }
    }

    private static void validateOperationReference(Set<String> operationIds, String operationId, String name) {
        if (!operationIds.contains(operationId)) {
            throw new IllegalArgumentException(name + " references an unknown operation");
        }
    }
}
