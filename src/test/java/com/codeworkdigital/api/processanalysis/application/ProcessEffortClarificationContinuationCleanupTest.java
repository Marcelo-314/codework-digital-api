package com.codeworkdigital.api.processanalysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class ProcessEffortClarificationContinuationCleanupTest {

    private static final Instant NOW = Instant.parse("2026-08-21T12:30:00Z");

    @Test
    void cleanExpiredUsesFixedClockInstantAsExactCutoffAndPropagatesCount() {
        RecordingRepository repository = new RecordingRepository();
        repository.deleted = 7;
        ProcessEffortClarificationContinuationCleanup cleanup =
                new ProcessEffortClarificationContinuationCleanup(
                        repository,
                        Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(cleanup.cleanExpired()).isEqualTo(7);
        assertThat(repository.cutoff).isEqualTo(NOW);
        assertThat(repository.deleteCalls).isEqualTo(1);
    }

    @Test
    void scheduledCleanupCadenceIsFifteenMinutesAndSeparateFromContinuationLifetime() throws Exception {
        Scheduled scheduled = ProcessEffortClarificationContinuationCleanup.class
                .getDeclaredMethod("cleanExpiredOnSchedule")
                .getAnnotation(Scheduled.class);

        assertThat(scheduled.fixedDelayString()).isEqualTo("PT15M");
        assertThat(scheduled.initialDelayString()).isEqualTo("PT15M");
        assertThat(ProcessEffortClarificationContinuationPolicy.P06_LAB_LIFETIME)
                .isEqualTo(java.time.Duration.ofMinutes(30));
    }

    @Test
    void cleanupDoesNotDependOnAnalysisModelOrResolverComponents() {
        assertThat(declaredFieldTypes(ProcessEffortClarificationContinuationCleanup.class))
                .containsExactly(
                        ProcessEffortClarificationContinuationRepository.class,
                        Clock.class);
        assertThat(constructorParameterTypes(ProcessEffortClarificationContinuationCleanup.class))
                .containsExactly(
                        ProcessEffortClarificationContinuationRepository.class,
                        Clock.class);
        assertThat(constructorParameterTypes(ProcessEffortClarificationContinuationCleanup.class))
                .doesNotContain(
                        ProcessAnalysisApplicationService.class,
                        ProcessAnalysisModelClient.class,
                        ProcessEffortClarificationResolver.class,
                        ProcessEffortClarificationContinuationResolutionService.class);
    }

    private List<Class<?>> declaredFieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .map(Field::getType)
                .filter(fieldType -> !fieldType.equals(String.class))
                .toList();
    }

    private List<Class<?>> constructorParameterTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredConstructors())
                .map(Constructor::getParameterTypes)
                .flatMap(Arrays::stream)
                .toList();
    }

    private static class RecordingRepository implements ProcessEffortClarificationContinuationRepository {

        private Instant cutoff;
        private int deleted;
        private int deleteCalls;

        @Override
        public void save(ProcessEffortClarificationContinuation continuation) {
            throw new UnsupportedOperationException("cleanup does not save continuations");
        }

        @Override
        public Optional<ProcessEffortClarificationContinuation> findById(
                ProcessEffortClarificationContinuationId id) {
            throw new UnsupportedOperationException("cleanup does not read continuations");
        }

        @Override
        public int deleteExpiredAtOrBefore(Instant cutoff) {
            this.cutoff = cutoff;
            deleteCalls++;
            return deleted;
        }

        @Override
        public boolean markResolvedIfActive(
                ProcessEffortClarificationContinuationId id,
                Instant resolvedAt) {
            throw new UnsupportedOperationException("cleanup does not resolve continuations");
        }
    }
}
