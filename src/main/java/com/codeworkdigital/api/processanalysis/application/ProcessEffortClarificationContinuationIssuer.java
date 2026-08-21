package com.codeworkdigital.api.processanalysis.application;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ProcessEffortClarificationContinuationIssuer {

    private final ProcessEffortClarificationContinuationRepository repository;
    private final Clock clock;

    public ProcessEffortClarificationContinuationIssuer(
            ProcessEffortClarificationContinuationRepository repository,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Optional<ProcessEffortClarificationContinuationId> issue(ProcessAnalysisResult result) {
        Objects.requireNonNull(result, "result");
        if (result.materialityEvidenceGaps().isEmpty()) {
            return Optional.empty();
        }
        ProcessEffortClarificationContext context = ProcessEffortClarificationContext.from(result);
        ProcessEffortClarificationContinuation continuation =
                ProcessEffortClarificationContinuation.create(context, clock);
        repository.save(continuation);
        return Optional.of(continuation.id());
    }
}
