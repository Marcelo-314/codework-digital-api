package com.codeworkdigital.api.processanalysis.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public record ProcessEffortClarificationContinuation(
        ProcessEffortClarificationContinuationId id,
        ProcessEffortClarificationContext context,
        Instant createdAt,
        Instant expiresAt) {

    public ProcessEffortClarificationContinuation {
        id = Objects.requireNonNull(id, "id");
        context = Objects.requireNonNull(context, "context");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("continuation expiresAt must be after createdAt");
        }
    }

    public static ProcessEffortClarificationContinuation create(
            ProcessEffortClarificationContext context,
            Clock clock) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(clock, "clock");
        Instant createdAt = clock.instant();
        return new ProcessEffortClarificationContinuation(
                ProcessEffortClarificationContinuationId.newId(),
                context,
                createdAt,
                createdAt.plus(ProcessEffortClarificationContinuationPolicy.P06_LAB_LIFETIME));
    }

    public boolean isExpired(Instant now) {
        Objects.requireNonNull(now, "now");
        return !now.isBefore(expiresAt);
    }
}
