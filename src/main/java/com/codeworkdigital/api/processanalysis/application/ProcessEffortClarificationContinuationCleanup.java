package com.codeworkdigital.api.processanalysis.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProcessEffortClarificationContinuationCleanup {

    private static final String CLEANUP_CADENCE = "PT15M";

    private final ProcessEffortClarificationContinuationRepository repository;
    private final Clock clock;

    public ProcessEffortClarificationContinuationCleanup(
            ProcessEffortClarificationContinuationRepository repository,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public int cleanExpired() {
        Instant cutoff = clock.instant();
        return repository.deleteExpiredAtOrBefore(cutoff);
    }

    @Scheduled(initialDelayString = CLEANUP_CADENCE, fixedDelayString = CLEANUP_CADENCE)
    void cleanExpiredOnSchedule() {
        cleanExpired();
    }
}
