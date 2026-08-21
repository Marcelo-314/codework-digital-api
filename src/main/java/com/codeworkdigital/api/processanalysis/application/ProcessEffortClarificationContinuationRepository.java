package com.codeworkdigital.api.processanalysis.application;

import java.time.Instant;
import java.util.Optional;

public interface ProcessEffortClarificationContinuationRepository {

    void save(ProcessEffortClarificationContinuation continuation);

    Optional<ProcessEffortClarificationContinuation> findById(
            ProcessEffortClarificationContinuationId id);

    int deleteExpiredAtOrBefore(Instant cutoff);

    boolean markResolvedIfActive(
            ProcessEffortClarificationContinuationId id,
            Instant resolvedAt);
}
