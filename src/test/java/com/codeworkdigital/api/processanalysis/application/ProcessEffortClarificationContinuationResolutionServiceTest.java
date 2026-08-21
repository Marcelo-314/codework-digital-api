package com.codeworkdigital.api.processanalysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeworkdigital.api.processanalysis.domain.ProcessAnalysisScope;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceGap;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceSource;
import com.codeworkdigital.api.processanalysis.domain.ProcessQuantityProjection;
import com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProcessEffortClarificationContinuationResolutionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-21T12:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-08-21T12:30:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final ProcessEffortEvidenceProjectionMapper sourceMapper = new ProcessEffortEvidenceProjectionMapper();

    @Test
    void activeVolumeContinuationWithCorrectAnswerResolves() {
        RecordingRepository repository = repository(continuation(contextWithVolumeGap()));
        ProcessEffortClarificationResolution resolution = service(repository).resolve(
                repository.continuation.id(),
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000")));

        assertThat(quantity(resolution).magnitude()).isEqualByComparingTo("8000");
        assertThat(resolution.materialityAssessment().status())
                .isEqualTo(ProcessEffortMaterialityAssessmentStatus.OPPORTUNITY_IDENTIFIED);
        assertThat(repository.markCalls).isEqualTo(1);
        assertThat(repository.continuation.resolvedAt()).contains(NOW);
    }

    @Test
    void activeEffortContinuationWithCorrectAnswerResolves() {
        RecordingRepository repository = repository(continuation(contextWithEffortGap()));
        ProcessEffortClarificationResolution resolution = service(repository).resolve(
                repository.continuation.id(),
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM, "2")));

        assertThat(quantity(resolution).magnitude()).isEqualByComparingTo("8000");
        assertThat(resolution.materialityAssessment().status())
                .isEqualTo(ProcessEffortMaterialityAssessmentStatus.OPPORTUNITY_IDENTIFIED);
    }

    @Test
    void bothGapContinuationWithCompleteAnswersResolves() {
        RecordingRepository repository = repository(continuation(contextWithBothGaps()));
        ProcessEffortClarificationResolution resolution = service(repository).resolve(
                repository.continuation.id(),
                List.of(
                        answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000"),
                        answer(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM, "2")));

        assertThat(quantity(resolution).magnitude()).isEqualByComparingTo("8000");
        assertThat(resolution.materialityAssessment().status())
                .isEqualTo(ProcessEffortMaterialityAssessmentStatus.OPPORTUNITY_IDENTIFIED);
    }

    @Test
    void belowThresholdAndZeroRemainValidNonMaterialResolutions() {
        RecordingRepository belowThreshold = repository(continuation(contextWithVolumeGap()));
        ProcessEffortClarificationResolution below = service(belowThreshold).resolve(
                belowThreshold.continuation.id(),
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4")));
        assertThat(quantity(below).magnitude()).isEqualByComparingTo("8");
        assertThat(below.materialityAssessment().status())
                .isEqualTo(ProcessEffortMaterialityAssessmentStatus.NO_MATERIAL_JUSTIFICATION_IDENTIFIED);

        RecordingRepository zeroRepository = repository(continuation(contextWithVolumeGap()));
        ProcessEffortClarificationResolution zero = service(zeroRepository).resolve(
                zeroRepository.continuation.id(),
                List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "0")));
        assertThat(quantity(zero).magnitude()).isEqualByComparingTo("0");
        assertThat(zero.materialityAssessment().status())
                .isEqualTo(ProcessEffortMaterialityAssessmentStatus.NO_MATERIAL_JUSTIFICATION_IDENTIFIED);
    }

    @Test
    void missingContinuationThrowsNotFound() {
        RecordingRepository repository = repository(null);

        assertThatThrownBy(() -> service(repository).resolve(
                        ProcessEffortClarificationContinuationId.newId(),
                        List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000"))))
                .isInstanceOf(ProcessEffortClarificationNotFoundException.class);
        assertThat(repository.markCalls).isZero();
    }

    @Test
    void expiredContinuationThrowsExpiredWithoutConsuming() {
        RecordingRepository repository = repository(new ProcessEffortClarificationContinuation(
                ProcessEffortClarificationContinuationId.newId(),
                contextWithVolumeGap(),
                NOW.minusSeconds(120),
                NOW,
                Optional.empty()));

        assertThatThrownBy(() -> service(repository).resolve(
                        repository.continuation.id(),
                        List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000"))))
                .isInstanceOf(ProcessEffortClarificationExpiredException.class);
        assertUnresolvedAndUnmarked(repository);
    }

    @Test
    void alreadyResolvedContinuationThrowsConflictWithoutConsuming() {
        RecordingRepository repository = repository(new ProcessEffortClarificationContinuation(
                ProcessEffortClarificationContinuationId.newId(),
                contextWithVolumeGap(),
                NOW.minusSeconds(120),
                EXPIRES_AT,
                Optional.of(NOW.minusSeconds(1))));

        assertThatThrownBy(() -> service(repository).resolve(
                        repository.continuation.id(),
                        List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000"))))
                .isInstanceOf(ProcessEffortClarificationAlreadyResolvedException.class);
        assertThat(repository.markCalls).isZero();
    }

    @Test
    void invalidAnswerSetsRemainRetryableAndUnresolved() {
        assertInvalidDoesNotConsume(List.of(), contextWithBothGaps());
        assertInvalidDoesNotConsume(
                List.of(
                        answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000"),
                        answer(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM, "2")),
                contextWithVolumeGap());
        assertInvalidDoesNotConsume(
                List.of(
                        answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000"),
                        answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "5000")),
                contextWithVolumeGap());
    }

    @Test
    void negativeAnswerConstructionFailsBeforeConsumption() {
        RecordingRepository repository = repository(continuation(contextWithVolumeGap()));

        assertThatThrownBy(() -> new ProcessEffortMaterialityClarificationAnswer(
                        ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD,
                        new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertUnresolvedAndUnmarked(repository);
    }

    @Test
    void successfulCalculationWithFailedAtomicTransitionThrowsLifecycleConflict() {
        RecordingRepository repository = repository(continuation(contextWithVolumeGap()));
        repository.markResult = false;

        assertThatThrownBy(() -> service(repository).resolve(
                        repository.continuation.id(),
                        List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000"))))
                .isInstanceOf(ProcessEffortClarificationLifecycleConflictException.class);
        assertThat(repository.markCalls).isEqualTo(1);
        assertThat(repository.continuation.resolvedAt()).isEmpty();
    }

    @Test
    void resolverFailureNeverCallsAtomicTransition() {
        RecordingRepository repository = repository(continuation(contextWithUnresolvableVolumeGap()));

        assertThatThrownBy(() -> service(repository).resolve(
                        repository.continuation.id(),
                        List.of(answer(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD, "4000"))))
                .isInstanceOf(ProcessEffortClarificationResolutionInvariantException.class);
        assertUnresolvedAndUnmarked(repository);
    }

    @Test
    void resolutionServiceDoesNotDependOnModelOrTechnologyFit() {
        assertThat(declaredFieldTypes(ProcessEffortClarificationContinuationResolutionService.class))
                .doesNotContain(ProcessAnalysisModelClient.class, TechnologyFitAssessmentEvaluator.class);
        assertThat(constructorParameterTypes(ProcessEffortClarificationContinuationResolutionService.class))
                .doesNotContain(ProcessAnalysisModelClient.class, TechnologyFitAssessmentEvaluator.class);
    }

    private void assertInvalidDoesNotConsume(
            List<ProcessEffortMaterialityClarificationAnswer> answers,
            ProcessEffortClarificationContext context) {
        RecordingRepository repository = repository(continuation(context));

        assertThatThrownBy(() -> service(repository).resolve(repository.continuation.id(), answers))
                .isInstanceOf(ProcessEffortClarificationAnswerValidationException.class);
        assertUnresolvedAndUnmarked(repository);
    }

    private void assertUnresolvedAndUnmarked(RecordingRepository repository) {
        assertThat(repository.markCalls).isZero();
        assertThat(repository.continuation.resolvedAt()).isEmpty();
    }

    private ProcessEffortClarificationContinuationResolutionService service(RecordingRepository repository) {
        return new ProcessEffortClarificationContinuationResolutionService(repository, resolver(), CLOCK);
    }

    private ProcessEffortClarificationResolver resolver() {
        return new ProcessEffortClarificationResolver(
                new ProcessEffortClarificationAnswerMaterializer(),
                new ProcessEffortEstablishedKnowledgeComposer(),
                new ProcessEffortPerReportingPeriodMaterializer(),
                new ProcessEffortMaterialityAssessmentEvaluator());
    }

    private RecordingRepository repository(ProcessEffortClarificationContinuation continuation) {
        return new RecordingRepository(continuation);
    }

    private ProcessQuantityProjection quantity(ProcessEffortClarificationResolution resolution) {
        return resolution.materialityAssessment().establishedOperationalBurden().orElseThrow();
    }

    private ProcessEffortMaterialityClarificationAnswer answer(
            ProcessEffortMaterialityEvidenceGapKind kind,
            String magnitude) {
        return new ProcessEffortMaterialityClarificationAnswer(kind, new BigDecimal(magnitude));
    }

    private ProcessEffortClarificationContinuation continuation(ProcessEffortClarificationContext context) {
        return new ProcessEffortClarificationContinuation(
                ProcessEffortClarificationContinuationId.newId(),
                context,
                NOW,
                EXPIRES_AT,
                Optional.empty());
    }

    private ProcessEffortClarificationContext contextWithVolumeGap() {
        return context(
                new ProcessEffortEvidence(
                        absent("ticket-ref", "ticket"),
                        exactEffort("2", "ticket-ref", "ticket")),
                List.of(gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD)));
    }

    private ProcessEffortClarificationContext contextWithEffortGap() {
        return context(
                new ProcessEffortEvidence(
                        exactVolume("4000", "ticket-ref", "ticket"),
                        absent("ticket-ref", "ticket")),
                List.of(gap(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM)));
    }

    private ProcessEffortClarificationContext contextWithBothGaps() {
        return context(
                new ProcessEffortEvidence(
                        absent("ticket-ref", "ticket"),
                        absent("ticket-ref", "ticket")),
                List.of(
                        gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD),
                        gap(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM)));
    }

    private ProcessEffortClarificationContext contextWithUnresolvableVolumeGap() {
        return context(
                new ProcessEffortEvidence(
                        absent("volume-item", "ticket"),
                        exactEffort("2", "effort-item", "ticket")),
                List.of(gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD)));
    }

    private ProcessEffortClarificationContext context(
            ProcessEffortEvidence evidence,
            List<ProcessEffortMaterialityEvidenceGap> gaps) {
        ProcessAnalysisResult mapped = sourceMapper.map(new ProcessAnalysisModelResult(understanding(), evidence));
        return ProcessEffortClarificationContext.from(new ProcessAnalysisResult(
                mapped.understanding(),
                mapped.effortEvidence(),
                mapped.volumeProjection(),
                mapped.effortProjection(),
                mapped.sourceKnowledge(),
                Optional.empty(),
                Optional.of(ProcessEffortMaterialityAssessment.notEstablished(
                        ProcessEffortMaterialityThreshold.P06_LAB_POLICY)),
                gaps,
                mapped.composable()));
    }

    private ProcessEffortMaterialityEvidenceGap gap(ProcessEffortMaterialityEvidenceGapKind kind) {
        return new ProcessEffortMaterialityEvidenceGap(
                kind,
                new ProcessEvidenceGap(
                        "question",
                        ProcessEvidenceSource.SELF_REPORTED,
                        ProcessEffortMaterialityEvidenceGapIdentifier.DECISION_AFFECTED,
                        ProcessAnalysisScope.processWide()));
    }

    private ProcessEffortEvidenceQuantity absent(String businessItemRef, String businessItemLabel) {
        return quantity(ProcessEffortEvidenceQuantityStatus.ABSENT, null, businessItemRef, businessItemLabel, null, null);
    }

    private ProcessEffortEvidenceQuantity exactVolume(
            String magnitude,
            String businessItemRef,
            String businessItemLabel) {
        return quantity(
                ProcessEffortEvidenceQuantityStatus.EXACT,
                magnitude,
                businessItemRef,
                businessItemLabel,
                ProcessReportingPeriodUnit.MONTH,
                null);
    }

    private ProcessEffortEvidenceQuantity exactEffort(
            String magnitude,
            String businessItemRef,
            String businessItemLabel) {
        return quantity(
                ProcessEffortEvidenceQuantityStatus.EXACT,
                magnitude,
                businessItemRef,
                businessItemLabel,
                null,
                ProcessEffortDurationUnit.MINUTE);
    }

    private ProcessEffortEvidenceQuantity quantity(
            ProcessEffortEvidenceQuantityStatus status,
            String magnitude,
            String businessItemRef,
            String businessItemLabel,
            ProcessReportingPeriodUnit reportingPeriod,
            ProcessEffortDurationUnit effortDuration) {
        return new ProcessEffortEvidenceQuantity(
                status,
                magnitude == null ? null : new BigDecimal(magnitude),
                null,
                null,
                businessItemRef,
                businessItemLabel,
                reportingPeriod,
                effortDuration,
                null,
                null);
    }

    private ProcessUnderstanding understanding() {
        return new ProcessUnderstanding(
                "Description",
                List.of("A request is received."),
                List.of("A manual check may occur."),
                List.of(),
                List.of(new ProcessUnderstandingStage(
                        "receive-request",
                        "Receive request",
                        "The team receives a request.",
                        ProcessStageProvenance.OBSERVED,
                        ProcessStageOperationType.RECEIVE,
                        ProcessStageInputNature.UNSTRUCTURED)),
                "This understanding is preliminary.");
    }

    private List<Class<?>> declaredFieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .map(Field::getType)
                .toList();
    }

    private List<Class<?>> constructorParameterTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredConstructors())
                .map(Constructor::getParameterTypes)
                .flatMap(Arrays::stream)
                .toList();
    }

    private static class RecordingRepository implements ProcessEffortClarificationContinuationRepository {

        private ProcessEffortClarificationContinuation continuation;
        private boolean markResult = true;
        private int markCalls;

        RecordingRepository(ProcessEffortClarificationContinuation continuation) {
            this.continuation = continuation;
        }

        @Override
        public void save(ProcessEffortClarificationContinuation continuation) {
            this.continuation = continuation;
        }

        @Override
        public Optional<ProcessEffortClarificationContinuation> findById(
                ProcessEffortClarificationContinuationId id) {
            return Optional.ofNullable(continuation)
                    .filter(candidate -> candidate.id().equals(id));
        }

        @Override
        public int deleteExpiredAtOrBefore(Instant cutoff) {
            throw new UnsupportedOperationException("resolution service does not delete continuations");
        }

        @Override
        public boolean markResolvedIfActive(
                ProcessEffortClarificationContinuationId id,
                Instant resolvedAt) {
            markCalls++;
            if (!markResult || continuation == null || !continuation.id().equals(id)
                    || continuation.isResolved() || continuation.isExpired(resolvedAt)) {
                return false;
            }
            continuation = new ProcessEffortClarificationContinuation(
                    continuation.id(),
                    continuation.context(),
                    continuation.createdAt(),
                    continuation.expiresAt(),
                    Optional.of(resolvedAt));
            return true;
        }
    }
}
