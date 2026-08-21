package com.codeworkdigital.api.processanalysis.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisModelResult;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisResult;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisStatus;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContinuation;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContinuationId;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContinuationRepository;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContext;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortEvidence;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortEvidenceProjectionMapper;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortEvidenceQuantity;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortEvidenceQuantityStatus;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortMaterialityAssessment;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortMaterialityAssessmentStatus;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortMaterialityEvidenceGap;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortMaterialityEvidenceGapKind;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortMaterialityThreshold;
import com.codeworkdigital.api.processanalysis.application.ProcessStageInputNature;
import com.codeworkdigital.api.processanalysis.application.ProcessStageOperationType;
import com.codeworkdigital.api.processanalysis.application.ProcessStageProvenance;
import com.codeworkdigital.api.processanalysis.application.ProcessUnderstanding;
import com.codeworkdigital.api.processanalysis.application.ProcessUnderstandingStage;
import com.codeworkdigital.api.processanalysis.domain.ProcessAnalysisScope;
import com.codeworkdigital.api.processanalysis.domain.ProcessBusinessItemPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerBusinessItemUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceArtifact;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceArtifactId;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceGap;
import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceSource;
import com.codeworkdigital.api.processanalysis.domain.ProcessKnownFact;
import com.codeworkdigital.api.processanalysis.domain.ProcessQuantityProjection;
import com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit;
import com.codeworkdigital.api.support.PostgreSqlIntegrationTestSupport;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(properties = "cwd.process-analysis.continuation-persistence-test=true")
class JdbcProcessEffortClarificationContinuationRepositoryIntegrationTest
        extends PostgreSqlIntegrationTestSupport {

    private static final Instant CREATED_AT = Instant.parse("2026-08-21T12:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-08-21T12:30:00Z");

    @Autowired
    private ProcessEffortClarificationContinuationRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    private final ProcessEffortEvidenceProjectionMapper sourceMapper = new ProcessEffortEvidenceProjectionMapper();

    @BeforeEach
    void cleanDatabase() {
        jdbcClient.sql("DELETE FROM process_effort_clarification_continuation").update();
    }

    @Test
    void flywayCreatesContinuationSchemaAfterContactMigration() {
        assertThat(toRegClass("public.flyway_schema_history")).isEqualTo("flyway_schema_history");
        assertThat(toRegClass("public.contact_submission")).isEqualTo("contact_submission");
        assertThat(toRegClass("public.process_effort_clarification_continuation"))
                .isEqualTo("process_effort_clarification_continuation");
        assertThat(successfulMigrationVersions()).contains("1", "2");
        assertThat(primaryKeyColumns()).containsExactly("id");
    }

    @Test
    void schemaRejectsInvalidLifecycleSchemaVersionAndDuplicateId() {
        ProcessEffortClarificationContinuation continuation =
                continuation(contextWithVolumeGap(), CREATED_AT, EXPIRES_AT);

        repository.save(continuation);

        assertThatThrownBy(() -> insertContinuation(
                        UUID.randomUUID(),
                        1,
                        "{}",
                        CREATED_AT,
                        CREATED_AT))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertContinuation(
                        UUID.randomUUID(),
                        2,
                        "{}",
                        CREATED_AT,
                        EXPIRES_AT))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> repository.save(continuation))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(repository.findById(continuation.id())).contains(continuation);
    }

    @Test
    void roundTripsActionableVolumeContinuation() {
        ProcessEffortClarificationContinuation saved =
                continuation(contextWithVolumeGap(), CREATED_AT, EXPIRES_AT);

        repository.save(saved);

        ProcessEffortClarificationContinuation found = repository.findById(saved.id()).orElseThrow();
        assertContinuationEnvelope(found, saved);
        assertContext(found.context(), saved.context());
        assertSourceKnowledge(found.context(), 1);
        assertGap(found.context().actionableGaps().getFirst(),
                ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD,
                "What monthly quantity do you use as the reference volume for this process?");
    }

    @Test
    void roundTripsActionableEffortContinuation() {
        ProcessEffortClarificationContinuation saved =
                continuation(contextWithEffortGap(), CREATED_AT, EXPIRES_AT);

        repository.save(saved);

        ProcessEffortClarificationContinuation found = repository.findById(saved.id()).orElseThrow();
        assertContinuationEnvelope(found, saved);
        assertContext(found.context(), saved.context());
        assertSourceKnowledge(found.context(), 1);
        assertGap(found.context().actionableGaps().getFirst(),
                ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM,
                "How many minutes of effort per processed business item do you use as the reference value?");
    }

    @Test
    void roundTripsBothGapContinuation() {
        ProcessEffortClarificationContinuation saved =
                continuation(contextWithBothGaps(), CREATED_AT, EXPIRES_AT);

        repository.save(saved);

        ProcessEffortClarificationContinuation found = repository.findById(saved.id()).orElseThrow();
        assertContinuationEnvelope(found, saved);
        assertContext(found.context(), saved.context());
        assertSourceKnowledge(found.context(), 0);
        assertThat(found.context().actionableGaps())
                .extracting(ProcessEffortMaterialityEvidenceGap::kind)
                .containsExactly(
                        ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD,
                        ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM);
        assertThat(found.context().actionableGaps())
                .extracting(gap -> gap.evidenceGap().question())
                .containsExactly(
                        "Que cantidad mensual usas como volumen de referencia para este proceso?",
                        "Cuantos minutos de esfuerzo por item de negocio procesado usas como valor de referencia?");
    }

    @Test
    void findByIdReturnsEmptyOnlyForMissingRow() {
        assertThat(repository.findById(ProcessEffortClarificationContinuationId.newId())).isEmpty();
    }

    @Test
    void expiredStoredContinuationIsReturnedWithoutCleanupOrFiltering() {
        Instant expiredAt = CREATED_AT.plusSeconds(60);
        ProcessEffortClarificationContinuation saved =
                continuation(contextWithVolumeGap(), CREATED_AT, expiredAt);

        repository.save(saved);

        ProcessEffortClarificationContinuation found = repository.findById(saved.id()).orElseThrow();
        assertThat(found.expiresAt()).isEqualTo(expiredAt);
        assertThat(found.isExpired(expiredAt)).isTrue();
        assertThat(found.isExpired(expiredAt.plusSeconds(1))).isTrue();
        assertThat(rowCount()).isEqualTo(1);
    }

    @Test
    void businessItemRefRemainsOnlyInsideContextPayload() {
        repository.save(continuation(contextWithBothGaps(), CREATED_AT, EXPIRES_AT));

        assertThat(tableColumns()).doesNotContain("business_item_ref");
        assertThat(payloadText())
                .contains("ticket-ref")
                .contains("businessItemRef");
    }

    @Test
    void unsupportedSchemaVersionCannotBeLoadedAsV1() {
        ProcessEffortClarificationContinuation saved =
                continuation(contextWithVolumeGap(), CREATED_AT, EXPIRES_AT);
        repository.save(saved);

        dropSchemaVersionConstraint();
        try {
            jdbcClient.sql("""
                            UPDATE process_effort_clarification_continuation
                            SET context_schema_version = 2
                            WHERE id = :id
                            """)
                    .param("id", saved.id().value())
                    .update();

            assertThatThrownBy(() -> repository.findById(saved.id()))
                    .isInstanceOf(ProcessEffortClarificationContinuationPersistenceException.class)
                    .hasMessageContaining("unsupported")
                    .hasMessageContaining(saved.id().value().toString());
        } finally {
            jdbcClient.sql("DELETE FROM process_effort_clarification_continuation").update();
            addSchemaVersionConstraint();
        }
    }

    @Test
    void structurallyInvalidPayloadDoesNotBecomeEmptyOptional() {
        ProcessEffortClarificationContinuationId id = ProcessEffortClarificationContinuationId.newId();
        insertContinuation(id.value(), 1, "{}", CREATED_AT, EXPIRES_AT);

        assertThatThrownBy(() -> repository.findById(id))
                .isInstanceOf(ProcessEffortClarificationContinuationPersistenceException.class)
                .hasMessageContaining(id.value().toString());
    }

    @Test
    void invalidPersistedContextCannotBypassContextInvariants() {
        ProcessEffortClarificationContinuation saved =
                continuation(contextWithVolumeGap(), CREATED_AT, EXPIRES_AT);
        repository.save(saved);
        jdbcClient.sql("""
                        UPDATE process_effort_clarification_continuation
                        SET context_payload = jsonb_set(context_payload, '{actionableGaps}', '[]'::jsonb)
                        WHERE id = :id
                        """)
                .param("id", saved.id().value())
                .update();

        assertThatThrownBy(() -> repository.findById(saved.id()))
                .isInstanceOf(ProcessEffortClarificationContinuationPersistenceException.class)
                .hasMessageContaining(saved.id().value().toString());
    }

    @Test
    void regressionBoundariesRemainUnchanged() throws Exception {
        String controller = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/api/ProcessAnalysisController.java"));
        String request = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/api/ProcessAnalysisRequest.java"));
        String response = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/api/ProcessAnalysisResponse.java"));
        String question = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/api/"
                        + "ProcessAnalysisClarificationQuestionResponse.java"));
        String service = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/application/ProcessAnalysisApplicationService.java"));
        String resolver = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/application/ProcessEffortClarificationResolver.java"));
        String materializer = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/application/"
                        + "ProcessEffortPerReportingPeriodMaterializer.java"));
        String repositorySource = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/codeworkdigital/api/processanalysis/infrastructure/persistence/"
                        + "JdbcProcessEffortClarificationContinuationRepository.java"));

        assertThat(controller + request + response + question)
                .doesNotContain("analysisId", "ProcessEffortClarificationContinuation");
        assertThat(service).doesNotContain("ProcessEffortClarificationContinuationRepository");
        assertThat(service.split("modelClient\\.analyze\\(", -1).length - 1).isEqualTo(1);
        assertThat(resolver).doesNotContain("ProcessEffortClarificationContinuation", "Repository", "DerivationVerifier");
        assertThat(materializer.split("\\.multiply\\(", -1).length - 1).isEqualTo(1);
        assertThat(repositorySource).doesNotContain("DELETE", "@Scheduled", "cleanup");
    }

    private void assertContinuationEnvelope(
            ProcessEffortClarificationContinuation found,
            ProcessEffortClarificationContinuation saved) {
        assertThat(found.id()).isEqualTo(saved.id());
        assertThat(found.createdAt()).isEqualTo(saved.createdAt());
        assertThat(found.expiresAt()).isEqualTo(saved.expiresAt());
    }

    private void assertContext(
            ProcessEffortClarificationContext found,
            ProcessEffortClarificationContext saved) {
        assertThat(found.effortEvidence()).isEqualTo(saved.effortEvidence());
        assertThat(found.effortEvidence().volumePerReportingPeriod().businessItemRef())
                .isEqualTo("ticket-ref");
        assertThat(found.effortEvidence().effortPerBusinessItem().businessItemRef())
                .isEqualTo("ticket-ref");
        assertThat(found.effortEvidence().volumePerReportingPeriod().businessItemLabel())
                .isEqualTo("ticket");
        assertThat(found.effortEvidence().effortPerBusinessItem().businessItemLabel())
                .isEqualTo("ticket");
        assertThat(found.materialityAssessment().status())
                .isEqualTo(ProcessEffortMaterialityAssessmentStatus.NOT_ESTABLISHED);
        assertThreshold(found.materialityAssessment().threshold());
    }

    private void assertSourceKnowledge(ProcessEffortClarificationContext context, int factCount) {
        assertThat(context.sourceKnowledge().evidenceBase().artifacts())
                .extracting(ProcessEvidenceArtifact::id)
                .extracting(ProcessEvidenceArtifactId::value)
                .containsExactly("source-process-description");
        assertThat(context.sourceKnowledge().knownFacts()).hasSize(factCount);
        assertThat(context.sourceKnowledge().knownFacts())
                .allSatisfy(fact -> {
                    assertThat(fact.evidenceArtifactIds())
                            .extracting(ProcessEvidenceArtifactId::value)
                            .containsExactly("source-process-description");
                    assertThat(fact.computableProjection()).isPresent();
                    assertThat(fact.scope()).isEqualTo(ProcessAnalysisScope.processWide());
                });
        if (factCount > 0) {
            ProcessKnownFact fact = context.sourceKnowledge().knownFacts().getFirst();
            ProcessQuantityProjection projection = (ProcessQuantityProjection) fact.computableProjection().orElseThrow();
            if (projection.unit() instanceof ProcessBusinessItemPerReportingPeriodUnit volumeUnit) {
                assertThat(volumeUnit.businessItemId().value()).isEqualTo("ticket-ref");
                assertThat(volumeUnit.reportingPeriod()).isEqualTo(ProcessReportingPeriodUnit.MONTH);
            }
            if (projection.unit() instanceof ProcessEffortPerBusinessItemUnit effortUnit) {
                assertThat(effortUnit.businessItemId().value()).isEqualTo("ticket-ref");
                assertThat(effortUnit.effortDuration()).isEqualTo(ProcessEffortDurationUnit.MINUTE);
            }
        }
    }

    private void assertThreshold(ProcessEffortMaterialityThreshold threshold) {
        ProcessQuantityProjection projection = threshold.projection();
        assertThat(projection.magnitude()).isEqualByComparingTo("2400");
        assertThat(projection.unit()).isInstanceOf(ProcessEffortPerReportingPeriodUnit.class);
        ProcessEffortPerReportingPeriodUnit unit = (ProcessEffortPerReportingPeriodUnit) projection.unit();
        assertThat(unit.effortDuration()).isEqualTo(ProcessEffortDurationUnit.MINUTE);
        assertThat(unit.reportingPeriod()).isEqualTo(ProcessReportingPeriodUnit.MONTH);
    }

    private void assertGap(
            ProcessEffortMaterialityEvidenceGap gap,
            ProcessEffortMaterialityEvidenceGapKind kind,
            String question) {
        assertThat(gap.kind()).isEqualTo(kind);
        ProcessEvidenceGap evidenceGap = gap.evidenceGap();
        assertThat(evidenceGap.question()).isEqualTo(question);
        assertThat(evidenceGap.source()).isEqualTo(ProcessEvidenceSource.SELF_REPORTED);
        assertThat(evidenceGap.decisionAffected())
                .isEqualTo("P06 materiality assessment cannot be established without this value");
        assertThat(evidenceGap.scope()).isEqualTo(ProcessAnalysisScope.processWide());
    }

    private ProcessEffortClarificationContinuation continuation(
            ProcessEffortClarificationContext context,
            Instant createdAt,
            Instant expiresAt) {
        return new ProcessEffortClarificationContinuation(
                ProcessEffortClarificationContinuationId.newId(),
                context,
                createdAt,
                expiresAt);
    }

    private ProcessEffortClarificationContext contextWithVolumeGap() {
        return context(
                new ProcessEffortEvidence(
                        absent("ticket-ref", "ticket"),
                        exactEffort("2", "ticket-ref", "ticket")),
                List.of(gap(
                        ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD,
                        "What monthly quantity do you use as the reference volume for this process?")));
    }

    private ProcessEffortClarificationContext contextWithEffortGap() {
        return context(
                new ProcessEffortEvidence(
                        exactVolume("4000", "ticket-ref", "ticket"),
                        absent("ticket-ref", "ticket")),
                List.of(gap(
                        ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM,
                        "How many minutes of effort per processed business item do you use as the reference value?")));
    }

    private ProcessEffortClarificationContext contextWithBothGaps() {
        return context(
                new ProcessEffortEvidence(
                        absent("ticket-ref", "ticket"),
                        absent("ticket-ref", "ticket")),
                List.of(
                        gap(
                                ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD,
                                "Que cantidad mensual usas como volumen de referencia para este proceso?"),
                        gap(
                                ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM,
                                "Cuantos minutos de esfuerzo por item de negocio procesado usas como valor de referencia?")));
    }

    private ProcessEffortClarificationContext context(
            ProcessEffortEvidence evidence,
            List<ProcessEffortMaterialityEvidenceGap> gaps) {
        ProcessAnalysisResult mapped = sourceMapper.map(new ProcessAnalysisModelResult(understanding(), evidence));
        return new ProcessEffortClarificationContext(
                mapped.effortEvidence(),
                mapped.sourceKnowledge(),
                ProcessEffortMaterialityAssessment.notEstablished(ProcessEffortMaterialityThreshold.P06_LAB_POLICY),
                gaps);
    }

    private ProcessEffortMaterialityEvidenceGap gap(
            ProcessEffortMaterialityEvidenceGapKind kind,
            String question) {
        return new ProcessEffortMaterialityEvidenceGap(
                kind,
                new ProcessEvidenceGap(
                        question,
                        ProcessEvidenceSource.SELF_REPORTED,
                        "P06 materiality assessment cannot be established without this value",
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
                ProcessAnalysisStatus.PROCESS_IDENTIFIED,
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

    private void insertContinuation(
            UUID id,
            int schemaVersion,
            String payload,
            Instant createdAt,
            Instant expiresAt) {
        jdbcClient.sql("""
                        INSERT INTO process_effort_clarification_continuation (
                            id,
                            context_schema_version,
                            context_payload,
                            created_at,
                            expires_at
                        )
                        VALUES (
                            :id,
                            :context_schema_version,
                            CAST(:context_payload AS jsonb),
                            :created_at,
                            :expires_at
                        )
                        """)
                .param("id", id)
                .param("context_schema_version", schemaVersion)
                .param("context_payload", payload)
                .param("created_at", timestamp(createdAt))
                .param("expires_at", timestamp(expiresAt))
                .update();
    }

    private OffsetDateTime timestamp(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private String toRegClass(String relation) {
        return jdbcClient.sql("SELECT to_regclass(:relation)::text")
                .param("relation", relation)
                .query(String.class)
                .single();
    }

    private List<String> successfulMigrationVersions() {
        return jdbcClient.sql("""
                        SELECT version
                        FROM flyway_schema_history
                        WHERE success = true
                        ORDER BY installed_rank
                        """)
                .query(String.class)
                .list();
    }

    private List<String> primaryKeyColumns() {
        return jdbcClient.sql("""
                        SELECT a.attname
                        FROM pg_index i
                        JOIN pg_attribute a
                          ON a.attrelid = i.indrelid
                         AND a.attnum = ANY(i.indkey)
                        WHERE i.indrelid = 'process_effort_clarification_continuation'::regclass
                          AND i.indisprimary
                        ORDER BY array_position(i.indkey, a.attnum)
                        """)
                .query(String.class)
                .list();
    }

    private List<String> tableColumns() {
        return jdbcClient.sql("""
                        SELECT column_name
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'process_effort_clarification_continuation'
                        ORDER BY ordinal_position
                        """)
                .query(String.class)
                .list();
    }

    private String payloadText() {
        return jdbcClient.sql("""
                        SELECT context_payload::text
                        FROM process_effort_clarification_continuation
                        LIMIT 1
                        """)
                .query(String.class)
                .single();
    }

    private int rowCount() {
        return jdbcClient.sql("SELECT count(*) FROM process_effort_clarification_continuation")
                .query(Integer.class)
                .single();
    }

    private void dropSchemaVersionConstraint() {
        jdbcClient.sql("""
                        ALTER TABLE process_effort_clarification_continuation
                        DROP CONSTRAINT ck_pecc_schema_version
                        """)
                .update();
    }

    private void addSchemaVersionConstraint() {
        jdbcClient.sql("""
                        ALTER TABLE process_effort_clarification_continuation
                        ADD CONSTRAINT ck_pecc_schema_version
                        CHECK (context_schema_version = 1)
                        """)
                .update();
    }
}
