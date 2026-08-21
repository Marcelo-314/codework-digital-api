package com.codeworkdigital.api.processanalysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeworkdigital.api.processanalysis.domain.ProcessAnalysisScope;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceGap;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceSource;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessEffortClarificationContinuationTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-21T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    private final ProcessEffortEvidenceProjectionMapper sourceMapper = new ProcessEffortEvidenceProjectionMapper();

    @Test
    void actionableContextRemainsValidAndPublicApplicationTypeForFutureAdapter() {
        ProcessAnalysisResult baseline = actionableBaseline();

        ProcessEffortClarificationContext context = ProcessEffortClarificationContext.from(baseline);

        assertThat(Modifier.isPublic(ProcessEffortClarificationContext.class.getModifiers())).isTrue();
        assertThat(ProcessEffortClarificationContext.class.getPackageName())
                .isEqualTo("com.codeworkdigital.api.processanalysis.application");
        assertThat(context.effortEvidence()).isSameAs(baseline.effortEvidence());
        assertThat(context.sourceKnowledge()).isSameAs(baseline.sourceKnowledge());
        assertThat(context.materialityAssessment()).isSameAs(baseline.materialityAssessment().orElseThrow());
        assertThat(context.actionableGaps())
                .extracting(ProcessEffortMaterialityEvidenceGap::kind)
                .containsExactly(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD);
    }

    @Test
    void continuationIdIsNonNullOpaqueUuidAndGeneratedIdsDiffer() {
        ProcessEffortClarificationContinuationId first = ProcessEffortClarificationContinuationId.newId();
        ProcessEffortClarificationContinuationId second = ProcessEffortClarificationContinuationId.newId();

        assertThat(first.value()).isInstanceOf(UUID.class);
        assertThat(first.value()).isNotNull();
        assertThat(second.value()).isNotNull();
        assertThat(first).isNotEqualTo(second);
        assertThat(ProcessEffortClarificationContinuationId.class.getRecordComponents())
                .singleElement()
                .satisfies(component -> {
                    assertThat(component.getName()).isEqualTo("value");
                    assertThat(component.getType()).isEqualTo(UUID.class);
                });
        assertThatThrownBy(() -> new ProcessEffortClarificationContinuationId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("value");
    }

    @Test
    void canonicalCreationUsesClockAndP06ThirtyMinuteLifetimeWithoutMutatingContext() {
        ProcessEffortClarificationContext context = ProcessEffortClarificationContext.from(actionableBaseline());
        List<ProcessEffortMaterialityEvidenceGap> originalGaps = context.actionableGaps();

        ProcessEffortClarificationContinuation continuation =
                ProcessEffortClarificationContinuation.create(context, FIXED_CLOCK);

        assertThat(ProcessEffortClarificationContinuationPolicy.P06_LAB_LIFETIME)
                .isEqualTo(Duration.ofMinutes(30));
        assertThat(continuation.id()).isNotNull();
        assertThat(continuation.context()).isSameAs(context);
        assertThat(continuation.createdAt()).isEqualTo(FIXED_NOW);
        assertThat(continuation.expiresAt()).isEqualTo(FIXED_NOW.plus(Duration.ofMinutes(30)));
        assertThat(continuation.resolvedAt()).isEmpty();
        assertThat(continuation.isResolved()).isFalse();
        assertThat(context.actionableGaps()).isSameAs(originalGaps);
        assertThat(context.actionableGaps())
                .extracting(ProcessEffortMaterialityEvidenceGap::kind)
                .containsExactly(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD);
    }

    @Test
    void expiryIsDeterministicAndDoesNotRefresh() {
        ProcessEffortClarificationContinuation continuation =
                ProcessEffortClarificationContinuation.create(
                        ProcessEffortClarificationContext.from(actionableBaseline()),
                        FIXED_CLOCK);
        Instant expiresAt = continuation.expiresAt();

        assertThat(continuation.isExpired(expiresAt.minusNanos(1))).isFalse();
        assertThat(continuation.isExpired(expiresAt)).isTrue();
        assertThat(continuation.isExpired(expiresAt.plusNanos(1))).isTrue();
        assertThat(continuation.expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void resolvedContinuationReportsResolvedWithoutChangingExpirySemantics() {
        ProcessEffortClarificationContext context = ProcessEffortClarificationContext.from(actionableBaseline());
        Instant resolvedAt = FIXED_NOW.plusSeconds(60);

        ProcessEffortClarificationContinuation continuation = new ProcessEffortClarificationContinuation(
                ProcessEffortClarificationContinuationId.newId(),
                context,
                FIXED_NOW,
                FIXED_NOW.plus(Duration.ofMinutes(30)),
                Optional.of(resolvedAt));

        assertThat(continuation.isResolved()).isTrue();
        assertThat(continuation.resolvedAt()).contains(resolvedAt);
        assertThat(continuation.isExpired(continuation.expiresAt())).isTrue();
    }

    @Test
    void invalidExpiresAtIsRejected() {
        ProcessEffortClarificationContext context = ProcessEffortClarificationContext.from(actionableBaseline());
        ProcessEffortClarificationContinuationId id = ProcessEffortClarificationContinuationId.newId();

        assertThatThrownBy(() -> new ProcessEffortClarificationContinuation(
                        id,
                        context,
                        FIXED_NOW,
                        FIXED_NOW,
                        Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiresAt");
        assertThatThrownBy(() -> new ProcessEffortClarificationContinuation(
                        id,
                        context,
                        FIXED_NOW,
                        FIXED_NOW.minusNanos(1),
                        Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiresAt");
    }

    @Test
    void invalidResolvedAtIsRejected() {
        ProcessEffortClarificationContext context = ProcessEffortClarificationContext.from(actionableBaseline());
        ProcessEffortClarificationContinuationId id = ProcessEffortClarificationContinuationId.newId();
        Instant expiresAt = FIXED_NOW.plus(Duration.ofMinutes(30));

        assertThatThrownBy(() -> new ProcessEffortClarificationContinuation(
                        id,
                        context,
                        FIXED_NOW,
                        expiresAt,
                        Optional.of(FIXED_NOW.minusNanos(1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resolvedAt");
        assertThatThrownBy(() -> new ProcessEffortClarificationContinuation(
                        id,
                        context,
                        FIXED_NOW,
                        expiresAt,
                        Optional.of(expiresAt)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resolvedAt");
        assertThatThrownBy(() -> new ProcessEffortClarificationContinuation(
                        id,
                        context,
                        FIXED_NOW,
                        expiresAt,
                        Optional.of(expiresAt.plusNanos(1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resolvedAt");
    }

    @Test
    void businessItemRefRemainsAvailableOnlyInsideTrustedContinuationContext() {
        ProcessEffortClarificationContinuation continuation =
                ProcessEffortClarificationContinuation.create(
                        ProcessEffortClarificationContext.from(actionableBaseline()),
                        FIXED_CLOCK);

        assertThat(continuation.context()
                        .effortEvidence()
                        .volumePerReportingPeriod()
                        .businessItemRef())
                .isEqualTo("ticket");
        assertThat(continuation.context()
                        .effortEvidence()
                        .effortPerBusinessItem()
                        .businessItemRef())
                .isEqualTo("ticket");
    }

    @Test
    void publicHttpApiDoesNotExposeContextContinuationOrBusinessItemRef() throws Exception {
        String controller = Files.readString(Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/api/ProcessAnalysisController.java"));
        String request = Files.readString(Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/api/ProcessAnalysisRequest.java"));
        String response = Files.readString(Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/api/ProcessAnalysisResponse.java"));
        String question = Files.readString(Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/api/"
                        + "ProcessAnalysisClarificationQuestionResponse.java"));

        assertThat(controller + request + response + question)
                .doesNotContain(
                        "ProcessEffortClarificationContext",
                        "ProcessEffortClarificationContinuationRepository",
                        "analysisId",
                        "businessItemRef",
                        "businessItemLabel",
                        "ProcessEffortMaterialityClarificationAnswer");
    }

    @Test
    void noBusinessItemRefComparisonAcrossContinuationIdsExists() throws Exception {
        String continuation = Files.readString(Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/application/"
                        + "ProcessEffortClarificationContinuation.java"));
        String continuationId = Files.readString(Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/application/"
                        + "ProcessEffortClarificationContinuationId.java"));
        String repository = Files.readString(Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/application/"
                        + "ProcessEffortClarificationContinuationRepository.java"));

        assertThat(continuation + continuationId + repository)
                .doesNotContain("businessItemRef", "ProcessBusinessItemUnitId");
    }

    @Test
    void repositoryPortContainsOnlyBoundedContinuationOperations() {
        assertThat(ProcessEffortClarificationContinuationRepository.class.isInterface()).isTrue();
        assertThat(Arrays.stream(ProcessEffortClarificationContinuationRepository.class.getDeclaredMethods())
                        .map(method -> method.getName())
                        .toList())
                .containsExactlyInAnyOrder(
                        "save",
                        "findById",
                        "deleteExpiredAtOrBefore",
                        "markResolvedIfActive");

        assertThat(parameterTypes("save"))
                .containsExactly(ProcessEffortClarificationContinuation.class);
        assertThat(returnType("save")).isEqualTo(void.class);
        assertThat(parameterTypes("findById"))
                .containsExactly(ProcessEffortClarificationContinuationId.class);
        assertThat(returnType("findById")).isEqualTo(Optional.class);
        assertThat(optionalReturnTypeArgument("findById"))
                .isEqualTo(ProcessEffortClarificationContinuation.class);
        assertThat(parameterTypes("deleteExpiredAtOrBefore"))
                .containsExactly(Instant.class);
        assertThat(returnType("deleteExpiredAtOrBefore")).isEqualTo(int.class);
        assertThat(parameterTypes("markResolvedIfActive"))
                .containsExactly(ProcessEffortClarificationContinuationId.class, Instant.class);
        assertThat(returnType("markResolvedIfActive")).isEqualTo(boolean.class);
    }

    @Test
    void continuationPersistenceIsWiredOnlyThroughIssuerInCurrentHttpFlow() throws Exception {
        String controller = Files.readString(Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/api/ProcessAnalysisController.java"));
        String applicationService = Files.readString(Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/application/ProcessAnalysisApplicationService.java"));
        String issuer = Files.readString(Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/application/"
                        + "ProcessEffortClarificationContinuationIssuer.java"));

        assertThat(controller)
                .contains("ProcessEffortClarificationContinuationIssuer", "continuationIssuer.issue(result)")
                .doesNotContain(
                        "ProcessEffortClarificationContinuationRepository",
                        "analysisId");
        assertThat(applicationService).doesNotContain(
                "ProcessEffortClarificationContinuation",
                "ProcessEffortClarificationContinuationRepository");
        assertThat(issuer)
                .contains("repository.save(continuation)")
                .doesNotContain("findById(", "ProcessEffortClarificationResolver");
    }

    @Test
    void resolverAndInferenceBoundariesRemainUnchanged() throws Exception {
        String resolver = Files.readString(Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/application/"
                        + "ProcessEffortClarificationResolver.java"));
        String applicationService = Files.readString(Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/application/"
                        + "ProcessAnalysisApplicationService.java"));
        String materializer = Files.readString(Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/application/"
                        + "ProcessEffortPerReportingPeriodMaterializer.java"));

        assertThat(resolver).doesNotContain("ProcessEffortClarificationContinuation", "DerivationVerifier");
        assertThat(applicationService.split("modelClient\\.analyze\\(", -1).length - 1).isEqualTo(1);
        assertThat(materializer.split("\\.multiply\\(", -1).length - 1).isEqualTo(1);
    }

    private List<Class<?>> parameterTypes(String methodName) {
        return Arrays.stream(repositoryMethod(methodName).getParameterTypes()).toList();
    }

    private Class<?> returnType(String methodName) {
        return repositoryMethod(methodName).getReturnType();
    }

    private Type optionalReturnTypeArgument(String methodName) {
        Type genericReturnType = repositoryMethod(methodName).getGenericReturnType();
        assertThat(genericReturnType).isInstanceOf(ParameterizedType.class);
        return ((ParameterizedType) genericReturnType).getActualTypeArguments()[0];
    }

    private Method repositoryMethod(String methodName) {
        return Arrays.stream(ProcessEffortClarificationContinuationRepository.class.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
    }

    private ProcessAnalysisResult actionableBaseline() {
        ProcessAnalysisResult mapped = sourceMapper.map(new ProcessAnalysisModelResult(
                understanding(),
                new ProcessEffortEvidence(
                        absent("ticket", "ticket"),
                        exactEffort("2", "ticket", "ticket"))));
        return new ProcessAnalysisResult(
                mapped.understanding(),
                mapped.effortEvidence(),
                mapped.volumeProjection(),
                mapped.effortProjection(),
                mapped.sourceKnowledge(),
                Optional.empty(),
                Optional.of(ProcessEffortMaterialityAssessment.notEstablished(
                        ProcessEffortMaterialityThreshold.P06_LAB_POLICY)),
                List.of(gap(ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD)),
                mapped.composable());
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
        return new ProcessEffortEvidenceQuantity(
                ProcessEffortEvidenceQuantityStatus.ABSENT,
                null,
                null,
                null,
                businessItemRef,
                businessItemLabel,
                null,
                null,
                null,
                null);
    }

    private ProcessEffortEvidenceQuantity exactEffort(
            String magnitude,
            String businessItemRef,
            String businessItemLabel) {
        return new ProcessEffortEvidenceQuantity(
                ProcessEffortEvidenceQuantityStatus.EXACT,
                new BigDecimal(magnitude),
                null,
                null,
                businessItemRef,
                businessItemLabel,
                null,
                ProcessEffortDurationUnit.MINUTE,
                null,
                null);
    }
}
