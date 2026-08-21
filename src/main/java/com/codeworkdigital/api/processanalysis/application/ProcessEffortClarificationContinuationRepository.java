package com.codeworkdigital.api.processanalysis.application;

import java.util.Optional;

public interface ProcessEffortClarificationContinuationRepository {

    void save(ProcessEffortClarificationContinuation continuation);

    Optional<ProcessEffortClarificationContinuation> findById(
            ProcessEffortClarificationContinuationId id);
}
