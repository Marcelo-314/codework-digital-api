package com.codeworkdigital.api.contact.infrastructure.persistence;

import com.codeworkdigital.api.contact.domain.ContactLocale;
import com.codeworkdigital.api.contact.domain.ContactSource;
import com.codeworkdigital.api.contact.domain.ContactStatus;
import com.codeworkdigital.api.contact.domain.ContactSubmission;
import com.codeworkdigital.api.contact.domain.ContactSubmissionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcContactSubmissionRepository implements ContactSubmissionRepository {

    private static final String COLUMNS = """
            id,
            idempotency_key,
            payload_hash,
            source,
            locale,
            name,
            email,
            phone,
            company_or_project,
            message,
            status,
            created_at,
            updated_at
            """;

    private final JdbcClient jdbcClient;

    public JdbcContactSubmissionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public ContactSubmission save(ContactSubmission submission) {
        return jdbcClient.sql("""
                        INSERT INTO contact_submission (
                            id,
                            idempotency_key,
                            payload_hash,
                            source,
                            locale,
                            name,
                            email,
                            phone,
                            company_or_project,
                            message,
                            status,
                            created_at,
                            updated_at
                        )
                        VALUES (
                            :id,
                            :idempotency_key,
                            :payload_hash,
                            :source,
                            :locale,
                            :name,
                            :email,
                            :phone,
                            :company_or_project,
                            :message,
                            :status,
                            :created_at,
                            :updated_at
                        )
                        RETURNING %s
                        """.formatted(COLUMNS))
                .param("id", submission.id())
                .param("idempotency_key", submission.idempotencyKey())
                .param("payload_hash", submission.payloadHash())
                .param("source", submission.source().name())
                .param("locale", submission.locale().name())
                .param("name", submission.name())
                .param("email", submission.email())
                .param("phone", submission.phone())
                .param("company_or_project", submission.companyOrProject())
                .param("message", submission.message())
                .param("status", submission.status().name())
                .param("created_at", OffsetDateTime.ofInstant(submission.createdAt(), ZoneOffset.UTC))
                .param("updated_at", OffsetDateTime.ofInstant(submission.updatedAt(), ZoneOffset.UTC))
                .query(this::mapRow)
                .single();
    }

    @Override
    public Optional<ContactSubmission> findById(UUID id) {
        return jdbcClient.sql("""
                        SELECT %s
                        FROM contact_submission
                        WHERE id = :id
                        """.formatted(COLUMNS))
                .param("id", id)
                .query(this::mapRow)
                .optional();
    }

    @Override
    public Optional<ContactSubmission> findByIdempotencyKey(UUID idempotencyKey) {
        return jdbcClient.sql("""
                        SELECT %s
                        FROM contact_submission
                        WHERE idempotency_key = :idempotency_key
                        """.formatted(COLUMNS))
                .param("idempotency_key", idempotencyKey)
                .query(this::mapRow)
                .optional();
    }

    @Override
    public List<ContactSubmission> findPageByCreatedAtDesc(int limit, long offset) {
        return jdbcClient.sql("""
                        SELECT %s
                        FROM contact_submission
                        ORDER BY created_at DESC, id DESC
                        LIMIT :limit
                        OFFSET :offset
                        """.formatted(COLUMNS))
                .param("limit", limit)
                .param("offset", offset)
                .query(this::mapRow)
                .list();
    }

    @Override
    public long count() {
        return jdbcClient.sql("SELECT count(*) FROM contact_submission")
                .query(Long.class)
                .single();
    }

    private ContactSubmission mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ContactSubmission(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("idempotency_key", UUID.class),
                resultSet.getString("payload_hash"),
                ContactSource.valueOf(resultSet.getString("source")),
                ContactLocale.valueOf(resultSet.getString("locale")),
                resultSet.getString("name"),
                resultSet.getString("email"),
                resultSet.getString("phone"),
                resultSet.getString("company_or_project"),
                resultSet.getString("message"),
                ContactStatus.valueOf(resultSet.getString("status")),
                resultSet.getObject("created_at", OffsetDateTime.class).toInstant(),
                resultSet.getObject("updated_at", OffsetDateTime.class).toInstant());
    }
}
