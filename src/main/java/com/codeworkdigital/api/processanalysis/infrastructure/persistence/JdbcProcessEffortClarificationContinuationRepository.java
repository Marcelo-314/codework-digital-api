package com.codeworkdigital.api.processanalysis.infrastructure.persistence;

import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContinuation;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContinuationId;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContinuationRepository;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContext;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Repository
public class JdbcProcessEffortClarificationContinuationRepository
        implements ProcessEffortClarificationContinuationRepository {

    private static final short CONTEXT_SCHEMA_VERSION = 1;
    private static final String COLUMNS = """
            id,
            context_schema_version,
            context_payload,
            created_at,
            expires_at,
            resolved_at
            """;

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public JdbcProcessEffortClarificationContinuationRepository(
            JdbcClient jdbcClient,
            ObjectMapper objectMapper) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public void save(ProcessEffortClarificationContinuation continuation) {
        Objects.requireNonNull(continuation, "continuation");
        jdbcClient.sql("""
                        INSERT INTO process_effort_clarification_continuation (
                            id,
                            context_schema_version,
                            context_payload,
                            created_at,
                            expires_at,
                            resolved_at
                        )
                        VALUES (
                            :id,
                            :context_schema_version,
                            CAST(:context_payload AS jsonb),
                            :created_at,
                            :expires_at,
                            NULL
                        )
                        """)
                .param("id", continuation.id().value())
                .param("context_schema_version", CONTEXT_SCHEMA_VERSION)
                .param("context_payload", serializeContext(continuation.context(), continuation.id()))
                .param("created_at", OffsetDateTime.ofInstant(continuation.createdAt(), ZoneOffset.UTC))
                .param("expires_at", OffsetDateTime.ofInstant(continuation.expiresAt(), ZoneOffset.UTC))
                .update();
    }

    @Override
    public Optional<ProcessEffortClarificationContinuation> findById(
            ProcessEffortClarificationContinuationId id) {
        Objects.requireNonNull(id, "id");
        return jdbcClient.sql("""
                        SELECT %s
                        FROM process_effort_clarification_continuation
                        WHERE id = :id
                        """.formatted(COLUMNS))
                .param("id", id.value())
                .query(this::mapRow)
                .optional();
    }

    @Override
    public boolean markResolvedIfActive(
            ProcessEffortClarificationContinuationId id,
            Instant resolvedAt) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(resolvedAt, "resolvedAt");
        int updated = jdbcClient.sql("""
                        UPDATE process_effort_clarification_continuation
                        SET resolved_at = :resolved_at
                        WHERE id = :id
                          AND resolved_at IS NULL
                          AND expires_at > :resolved_at
                        """)
                .param("id", id.value())
                .param("resolved_at", OffsetDateTime.ofInstant(resolvedAt, ZoneOffset.UTC))
                .update();
        return updated == 1;
    }

    private ProcessEffortClarificationContinuation mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        ProcessEffortClarificationContinuationId id =
                new ProcessEffortClarificationContinuationId(resultSet.getObject("id", java.util.UUID.class));
        short schemaVersion = resultSet.getShort("context_schema_version");
        if (schemaVersion != CONTEXT_SCHEMA_VERSION) {
            throw new ProcessEffortClarificationContinuationPersistenceException(
                    "unsupported process effort clarification continuation schema version %d for id %s"
                            .formatted(schemaVersion, id.value()));
        }
        ProcessEffortClarificationContext context = deserializeContext(
                resultSet.getString("context_payload"),
                id);
        return new ProcessEffortClarificationContinuation(
                id,
                context,
                resultSet.getObject("created_at", OffsetDateTime.class).toInstant(),
                resultSet.getObject("expires_at", OffsetDateTime.class).toInstant(),
                Optional.ofNullable(resultSet.getObject("resolved_at", OffsetDateTime.class))
                        .map(OffsetDateTime::toInstant));
    }

    private String serializeContext(
            ProcessEffortClarificationContext context,
            ProcessEffortClarificationContinuationId id) {
        try {
            return objectMapper.writeValueAsString(ProcessEffortClarificationContextSnapshotV1.from(context));
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new ProcessEffortClarificationContinuationPersistenceException(
                    "failed to serialize process effort clarification continuation context for id " + id.value(),
                    exception);
        }
    }

    private ProcessEffortClarificationContext deserializeContext(
            String payload,
            ProcessEffortClarificationContinuationId id) {
        try {
            ProcessEffortClarificationContextSnapshotV1 snapshot =
                    objectMapper.readValue(payload, ProcessEffortClarificationContextSnapshotV1.class);
            return snapshot.toContext();
        } catch (JacksonException | IllegalArgumentException | NullPointerException exception) {
            throw new ProcessEffortClarificationContinuationPersistenceException(
                    "failed to reconstruct process effort clarification continuation context for id " + id.value(),
                    exception);
        }
    }
}
