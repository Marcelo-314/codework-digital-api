package com.codeworkdigital.api.contact.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeworkdigital.api.contact.domain.ContactLocale;
import com.codeworkdigital.api.contact.domain.ContactSource;
import com.codeworkdigital.api.contact.domain.ContactStatus;
import com.codeworkdigital.api.contact.domain.ContactSubmission;
import com.codeworkdigital.api.contact.domain.ContactSubmissionRepository;
import com.codeworkdigital.api.support.PostgreSqlIntegrationTestSupport;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest
class JdbcContactSubmissionRepositoryIntegrationTest extends PostgreSqlIntegrationTestSupport {

    private static final String VALID_HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Autowired
    private ContactSubmissionRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void cleanDatabase() {
        jdbcClient.sql("DELETE FROM contact_submission").update();
    }

    @Test
    void flywayCreatesSchemaFromEmptyDatabase() {
        assertThat(toRegClass("public.flyway_schema_history")).isEqualTo("flyway_schema_history");
        assertThat(toRegClass("public.contact_submission")).isEqualTo("contact_submission");
        assertThat(jdbcClient.sql("""
                        SELECT success
                        FROM flyway_schema_history
                        WHERE version = '1'
                        """)
                .query(Boolean.class)
                .single()).isTrue();
    }

    @Test
    void savePersistsAndReturnsContactSubmission() {
        ContactSubmission submission = validSubmission(null, null);

        ContactSubmission saved = repository.save(submission);

        assertThat(saved).isEqualTo(submission);
        assertThat(repository.findById(submission.id())).contains(submission);
    }

    @Test
    void savePreservesOptionalValuesWhenPresent() {
        ContactSubmission submission = validSubmission("+54 11 5555 0101", "CodeWork Digital");

        ContactSubmission saved = repository.save(submission);

        assertThat(saved.phone()).isEqualTo("+54 11 5555 0101");
        assertThat(saved.companyOrProject()).isEqualTo("CodeWork Digital");
    }

    @Test
    void findByIdReturnsEmptyForMissingSubmission() {
        Optional<ContactSubmission> found = repository.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void findByIdempotencyKeyRecoversExistingSubmissionAndReturnsEmptyForMissingKey() {
        ContactSubmission submission = repository.save(validSubmission(null, null));

        assertThat(repository.findByIdempotencyKey(submission.idempotencyKey())).contains(submission);
        assertThat(repository.findByIdempotencyKey(UUID.randomUUID())).isEmpty();
    }

    @Test
    void duplicateIdempotencyKeyFailsAndKeepsOriginalRow() {
        ContactSubmission first = repository.save(validSubmission(null, null));
        ContactSubmission duplicate = new ContactSubmission(
                UUID.randomUUID(),
                first.idempotencyKey(),
                first.payloadHash(),
                first.source(),
                first.locale(),
                "Different Person",
                "different@example.test",
                null,
                null,
                "Different message",
                first.status(),
                first.createdAt(),
                first.updatedAt());

        assertThatThrownBy(() -> repository.save(duplicate)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(repository.findById(first.id())).contains(first);
        assertThat(repository.findById(duplicate.id())).isEmpty();
    }

    @Test
    void constraintsRejectInvalidValues() {
        assertConstraintViolation("source", "LANDING");
        assertConstraintViolation("locale", "PT");
        assertConstraintViolation("status", "QUALIFIED");
        assertConstraintViolation("payload_hash", "abcdef");
        assertConstraintViolation("name", "   ");
        assertConstraintViolation("message", "   ");
        assertConstraintViolation("updated_at", Instant.parse("2026-08-04T11:59:59Z"));
    }

    private String toRegClass(String relation) {
        return jdbcClient.sql("SELECT to_regclass(:relation)::text")
                .param("relation", relation)
                .query(String.class)
                .single();
    }

    private void assertConstraintViolation(String column, Object invalidValue) {
        ContactSubmission base = validSubmission(null, null);

        assertThatThrownBy(() -> insertWithOverride(base, column, invalidValue))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertWithOverride(ContactSubmission submission, String column, Object invalidValue) {
        jdbcClient.sql("""
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
                        """)
                .param("id", submission.id())
                .param("idempotency_key", submission.idempotencyKey())
                .param("payload_hash", valueFor(column, "payload_hash", invalidValue, submission.payloadHash()))
                .param("source", valueFor(column, "source", invalidValue, submission.source().name()))
                .param("locale", valueFor(column, "locale", invalidValue, submission.locale().name()))
                .param("name", valueFor(column, "name", invalidValue, submission.name()))
                .param("email", submission.email())
                .param("phone", submission.phone())
                .param("company_or_project", submission.companyOrProject())
                .param("message", valueFor(column, "message", invalidValue, submission.message()))
                .param("status", valueFor(column, "status", invalidValue, submission.status().name()))
                .param("created_at", timestampParameter(submission.createdAt()))
                .param("updated_at", valueFor(column, "updated_at", timestampParameter(invalidValue), timestampParameter(submission.updatedAt())))
                .update();
    }

    private Object timestampParameter(Object value) {
        return value instanceof Instant instant ? OffsetDateTime.ofInstant(instant, ZoneOffset.UTC) : value;
    }

    private Object valueFor(String invalidColumn, String candidateColumn, Object invalidValue, Object validValue) {
        return invalidColumn.equals(candidateColumn) ? invalidValue : validValue;
    }

    private ContactSubmission validSubmission(String phone, String companyOrProject) {
        Instant timestamp = Instant.parse("2026-08-04T12:00:00Z");
        return new ContactSubmission(
                UUID.randomUUID(),
                UUID.randomUUID(),
                VALID_HASH,
                ContactSource.HOME,
                ContactLocale.ES,
                "Ada Lovelace",
                "ada@example.test",
                phone,
                companyOrProject,
                "I would like to discuss a project.",
                ContactStatus.RECEIVED,
                timestamp,
                timestamp);
    }
}
