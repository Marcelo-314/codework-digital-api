package com.codeworkdigital.api.processanalysis.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record ProcessEffortClarificationContinuation(
        ProcessEffortClarificationContinuationId id,
        ProcessEffortClarificationContext context,
        Instant createdAt,
        Instant expiresAt,
        Optional<Instant> resolvedAt) {

    public ProcessEffortClarificationContinuation {
        id = Objects.requireNonNull(id, "id");
        context = Objects.requireNonNull(context, "context");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        resolvedAt = Objects.requireNonNull(resolvedAt, "resolvedAt");
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("continuation expiresAt must be after createdAt");
        }
        if (resolvedAt.isPresent()) {
            Instant resolved = resolvedAt.get();
            if (resolved.isBefore(createdAt)) {
                throw new IllegalArgumentException("continuation resolvedAt must not be before createdAt");
            }
            if (!resolved.isBefore(expiresAt)) {
                throw new IllegalArgumentException("continuation resolvedAt must be before expiresAt");
            }
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
                createdAt.plus(ProcessEffortClarificationContinuationPolicy.P06_LAB_LIFETIME),
                Optional.empty());
    }

    public boolean isExpired(Instant now) {
        Objects.requireNonNull(now, "now");
        return !now.isBefore(expiresAt);
    }

    public boolean isResolved() {
        return resolvedAt.isPresent();
    }
}
