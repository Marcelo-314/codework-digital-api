package com.codeworkdigital.api.processanalysis.application;

import java.util.Objects;
import java.util.UUID;

public record ProcessEffortClarificationContinuationId(UUID value) {

    public ProcessEffortClarificationContinuationId {
        value = Objects.requireNonNull(value, "value");
    }

    public static ProcessEffortClarificationContinuationId newId() {
        return new ProcessEffortClarificationContinuationId(UUID.randomUUID());
    }
}
