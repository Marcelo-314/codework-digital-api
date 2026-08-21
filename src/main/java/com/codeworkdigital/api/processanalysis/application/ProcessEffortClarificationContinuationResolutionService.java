package com.codeworkdigital.api.processanalysis.application;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ProcessEffortClarificationContinuationResolutionService {

    private final ProcessEffortClarificationContinuationRepository repository;
    private final ProcessEffortClarificationResolver resolver;
    private final Clock clock;

    public ProcessEffortClarificationContinuationResolutionService(
            ProcessEffortClarificationContinuationRepository repository,
            ProcessEffortClarificationResolver resolver,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public ProcessEffortClarificationResolution resolve(
            ProcessEffortClarificationContinuationId id,
            List<ProcessEffortMaterialityClarificationAnswer> answers) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(answers, "answers");

        ProcessEffortClarificationContinuation continuation = repository.findById(id)
                .orElseThrow(ProcessEffortClarificationNotFoundException::new);
        if (continuation.isResolved()) {
            throw new ProcessEffortClarificationAlreadyResolvedException();
        }

        Instant now = clock.instant();
        if (continuation.isExpired(now)) {
            throw new ProcessEffortClarificationExpiredException();
        }

        validateAnswerSet(continuation.context(), answers);

        ProcessEffortClarificationResolution resolution;
        try {
            resolution = resolver.resolve(continuation.context(), answers);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ProcessEffortClarificationResolutionInvariantException(exception);
        }

        Instant resolvedAt = clock.instant();
        if (repository.markResolvedIfActive(id, resolvedAt)) {
            return resolution;
        }
        throw new ProcessEffortClarificationLifecycleConflictException();
    }

    private static void validateAnswerSet(
            ProcessEffortClarificationContext context,
            List<ProcessEffortMaterialityClarificationAnswer> answers) {
        Set<ProcessEffortMaterialityEvidenceGapKind> required =
                EnumSet.noneOf(ProcessEffortMaterialityEvidenceGapKind.class);
        context.actionableGaps().forEach(gap -> required.add(gap.kind()));

        Set<ProcessEffortMaterialityEvidenceGapKind> supplied =
                EnumSet.noneOf(ProcessEffortMaterialityEvidenceGapKind.class);
        for (ProcessEffortMaterialityClarificationAnswer answer : answers) {
            if (!supplied.add(answer.kind())) {
                throw new ProcessEffortClarificationAnswerValidationException();
            }
        }
        if (!supplied.equals(required)) {
            throw new ProcessEffortClarificationAnswerValidationException();
        }
    }
}
