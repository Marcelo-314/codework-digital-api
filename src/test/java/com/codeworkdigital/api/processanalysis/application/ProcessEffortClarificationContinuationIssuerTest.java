package com.codeworkdigital.api.processanalysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeworkdigital.api.processanalysis.domain.ProcessAnalysisScope;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceGap;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceSource;
import com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProcessEffortClarificationContinuationIssuerTest {

    private static final Instant NOW = Instant.parse("2026-08-21T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final ProcessEffortEvidenceProjectionMapper sourceMapper = new ProcessEffortEvidenceProjectionMapper();
    private final ProcessEffortPerReportingPeriodMaterializer materializer =
            new ProcessEffortPerReportingPeriodMaterializer();
    private final ProcessEffortMaterialityAssessmentEvaluator assessmentEvaluator =
            new ProcessEffortMaterialityAssessmentEvaluator();

    @Test
    void actionableVolumeResultCreatesExactlyOneContinuation() {
        RecordingRepository repository = new RecordingRepository();
        ProcessEffortClarificationContinuationIssuer issuer =
                new ProcessEffortClarificationContinuationIssuer(repository, CLOCK);
        ProcessAnalysisResult result = actionableVolumeResult();

        Optional<ProcessEffortClarificationContinuationId> id = issuer.issue(result);

        assertIssuedContinuation(repository, result, id);
        assertThat(repository.saved.getFirst().context().actionableGaps())
                .extracting(ProcessEffortMaterialityEvidenceGap::kind)
                .containsExactly(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD);
    }

    @Test
    void actionableEffortResultCreatesExactlyOneContinuation() {
        RecordingRepository repository = new RecordingRepository();
        ProcessEffortClarificationContinuationIssuer issuer =
                new ProcessEffortClarificationContinuationIssuer(repository, CLOCK);
        ProcessAnalysisResult result = actionableEffortResult();

        Optional<ProcessEffortClarificationContinuationId> id = issuer.issue(result);

        assertIssuedContinuation(repository, result, id);
        assertThat(repository.saved.getFirst().context().actionableGaps())
                .extracting(ProcessEffortMaterialityEvidenceGap::kind)
                .containsExactly(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM);
    }

    @Test
    void bothGapResultCreatesExactlyOneContinuation() {
        RecordingRepository repository = new RecordingRepository();
        ProcessEffortClarificationContinuationIssuer issuer =
                new ProcessEffortClarificationContinuationIssuer(repository, CLOCK);
        ProcessAnalysisResult result = bothGapResult();

        Optional<ProcessEffortClarificationContinuationId> id = issuer.issue(result);

        assertIssuedContinuation(repository, result, id);
        assertThat(repository.saved.getFirst().context().actionableGaps())
                .extracting(ProcessEffortMaterialityEvidenceGap::kind)
                .containsExactly(
                        ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD,
                        ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM);
    }

    @Test
    void noGapResultReturnsEmptyAndPerformsNoRepositoryWrites() {
        RecordingRepository repository = new RecordingRepository();
        ProcessEffortClarificationContinuationIssuer issuer =
                new ProcessEffortClarificationContinuationIssuer(repository, CLOCK);

        Optional<ProcessEffortClarificationContinuationId> id = issuer.issue(noGapResult());

        assertThat(id).isEmpty();
        assertThat(repository.saved).isEmpty();
    }

    @Test
    void nonProcessResultPerformsNoRepositoryWrites() {
        RecordingRepository repository = new RecordingRepository();
        ProcessEffortClarificationContinuationIssuer issuer =
                new ProcessEffortClarificationContinuationIssuer(repository, CLOCK);

        Optional<ProcessEffortClarificationContinuationId> id = issuer.issue(nonProcessResult());

        assertThat(id).isEmpty();
        assertThat(repository.saved).isEmpty();
    }

    @Test
    void nonActionableNotEstablishedResultPerformsNoRepositoryWrites() {
        RecordingRepository repository = new RecordingRepository();
        ProcessEffortClarificationContinuationIssuer issuer =
                new ProcessEffortClarificationContinuationIssuer(repository, CLOCK);

        Optional<ProcessEffortClarificationContinuationId> id = issuer.issue(nonActionableNotEstablishedResult());

        assertThat(id).isEmpty();
        assertThat(repository.saved).isEmpty();
    }

    @Test
    void repositoryFailurePropagatesWithoutReturningSuccessfulId() {
        RecordingRepository repository = new RecordingRepository();
        repository.failure = new IllegalStateException("save failed");
        ProcessEffortClarificationContinuationIssuer issuer =
                new ProcessEffortClarificationContinuationIssuer(repository, CLOCK);

        assertThatThrownBy(() -> issuer.issue(actionableVolumeResult()))
                .isSameAs(repository.failure);
        assertThat(repository.saved).isEmpty();
    }

    private void assertIssuedContinuation(
            RecordingRepository repository,
            ProcessAnalysisResult result,
            Optional<ProcessEffortClarificationContinuationId> id) {
        assertThat(repository.saved).hasSize(1);
        ProcessEffortClarificationContinuation saved = repository.saved.getFirst();
        assertThat(id).contains(saved.id());
        assertThat(saved.createdAt()).isEqualTo(NOW);
        assertThat(saved.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
        assertThat(saved.context()).isEqualTo(ProcessEffortClarificationContext.from(result));
    }

    private ProcessAnalysisResult actionableVolumeResult() {
        return actionableResult(
                new ProcessEffortEvidence(
                        absent("item-1", "ticket"),
                        exactEffort("3", "item-1", "ticket")),
                List.of(gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD)));
    }

    private ProcessAnalysisResult actionableEffortResult() {
        return actionableResult(
                new ProcessEffortEvidence(
                        exactVolume("4000", "item-1", "ticket"),
                        absent("item-1", "ticket")),
                List.of(gap(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM)));
    }

    private ProcessAnalysisResult bothGapResult() {
        return actionableResult(
                new ProcessEffortEvidence(
                        absent("item-1", "ticket"),
                        absent("item-1", "ticket")),
                List.of(
                        gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD),
                        gap(ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM)));
    }

    private ProcessAnalysisResult noGapResult() {
        ProcessAnalysisResult mapped = sourceMapper.map(new ProcessAnalysisModelResult(
                processUnderstanding(),
                new ProcessEffortEvidence(
                        exactVolume("4", "item-1", "ticket"),
                        exactEffort("3", "item-1", "ticket"))));
        ProcessAnalysisResult materialized = mapped.withDerivedResult(materializer.materialize(mapped.sourceKnowledge()));
        return materialized.withMaterialityAssessment(assessmentEvaluator.assess(materialized.derivedResult()));
    }

    private ProcessAnalysisResult nonProcessResult() {
        ProcessAnalysisResult mapped = sourceMapper.map(new ProcessAnalysisModelResult(
                new ProcessUnderstanding(
                        "We want AI.",
                        ProcessAnalysisStatus.OUT_OF_SCOPE,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        ""),
                ProcessEffortEvidence.empty()));
        return mapped.withMaterialityAssessment(ProcessEffortMaterialityAssessment.notEstablished(
                ProcessEffortMaterialityThreshold.P06_LAB_POLICY));
    }

    private ProcessAnalysisResult nonActionableNotEstablishedResult() {
        ProcessAnalysisResult mapped = sourceMapper.map(new ProcessAnalysisModelResult(
                processUnderstanding(),
                new ProcessEffortEvidence(
                        absent("item-A", "ticket"),
                        exactEffort("3", "item-B", "ticket"))));
        return mapped.withMaterialityAssessment(ProcessEffortMaterialityAssessment.notEstablished(
                ProcessEffortMaterialityThreshold.P06_LAB_POLICY));
    }

    private ProcessAnalysisResult actionableResult(
            ProcessEffortEvidence evidence,
            List<ProcessEffortMaterialityEvidenceGap> gaps) {
        ProcessAnalysisResult mapped = sourceMapper.map(new ProcessAnalysisModelResult(processUnderstanding(), evidence));
        return new ProcessAnalysisResult(
                mapped.understanding(),
                mapped.effortEvidence(),
                mapped.volumeProjection(),
                mapped.effortProjection(),
                mapped.sourceKnowledge(),
                Optional.empty(),
                Optional.of(ProcessEffortMaterialityAssessment.notEstablished(
                        ProcessEffortMaterialityThreshold.P06_LAB_POLICY)),
                gaps,
                mapped.composable());
    }

    private ProcessUnderstanding processUnderstanding() {
        return new ProcessUnderstanding(
                "Receive requests and validate stock.",
                List.of("A request arrives."),
                List.of("Manual validation may be involved."),
                List.of(),
                List.of(new ProcessUnderstandingStage(
                        "receive-request",
                        "Receive request",
                        "The team receives a request.",
                        ProcessStageProvenance.OBSERVED,
                        ProcessStageOperationType.RECEIVE,
                        ProcessStageInputNature.UNSTRUCTURED)),
                "Preliminary.");
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

    private static final class RecordingRepository implements ProcessEffortClarificationContinuationRepository {

        private final List<ProcessEffortClarificationContinuation> saved = new java.util.ArrayList<>();
        private RuntimeException failure;

        @Override
        public void save(ProcessEffortClarificationContinuation continuation) {
            if (failure != null) {
                throw failure;
            }
            saved.add(continuation);
        }

        @Override
        public Optional<ProcessEffortClarificationContinuation> findById(
                ProcessEffortClarificationContinuationId id) {
            throw new UnsupportedOperationException("issuer does not read continuations");
        }

        @Override
        public boolean markResolvedIfActive(
                ProcessEffortClarificationContinuationId id,
                Instant resolvedAt) {
            throw new UnsupportedOperationException("issuer does not resolve continuations");
        }
    }
}
