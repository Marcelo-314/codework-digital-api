package com.codeworkdigital.api.contact.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeworkdigital.api.contact.domain.ContactLocale;
import com.codeworkdigital.api.contact.domain.ContactSource;
import com.codeworkdigital.api.contact.domain.ContactStatus;
import com.codeworkdigital.api.contact.domain.ContactSubmission;
import com.codeworkdigital.api.contact.domain.ContactSubmissionRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

class ContactSubmissionApplicationServiceTest {

    private static final Instant CLOCK_INSTANT = Instant.parse("2026-08-05T10:15:30.123456789Z");
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    private final ContactSubmissionNormalizer normalizer = new ContactSubmissionNormalizer();
    private final ContactSubmissionPayloadHasher hasher = new ContactSubmissionPayloadHasher();
    private final Clock clock = Clock.fixed(CLOCK_INSTANT, ZoneOffset.UTC);

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void createsNewSubmission() {
        FakeRepository repository = new FakeRepository();
        ContactSubmissionApplicationService service = service(repository);

        SubmitContactSubmissionResult result = service.submit(command("  Ada   Lovelace  ", "Message"));

        ContactSubmission saved = repository.saved.values().iterator().next();
        assertThat(result.created()).isTrue();
        assertThat(result.submissionId()).isEqualTo(saved.id());
        assertThat(result.status()).isEqualTo(ContactStatus.RECEIVED);
        assertThat(saved.name()).isEqualTo("Ada Lovelace");
        assertThat(saved.createdAt()).isEqualTo(CLOCK_INSTANT.truncatedTo(ChronoUnit.MICROS));
        assertThat(saved.updatedAt()).isEqualTo(saved.createdAt());
        assertThat(saved.createdAt().getNano() % 1_000).isZero();
    }

    @Test
    void replaysExistingSubmissionWithSameNormalizedPayload() {
        FakeRepository repository = new FakeRepository();
        ContactSubmissionApplicationService service = service(repository);
        SubmitContactSubmissionCommand first = command("Ada Lovelace", "Message");
        SubmitContactSubmissionResult created = service.submit(first);

        SubmitContactSubmissionResult replay = service.submit(command(first.idempotencyKey(), "  Ada   Lovelace  ", "Message"));

        assertThat(replay.created()).isFalse();
        assertThat(replay.submissionId()).isEqualTo(created.submissionId());
        assertThat(replay.receivedAt()).isEqualTo(created.receivedAt());
        assertThat(repository.saveCalls).isEqualTo(1);
    }

    @Test
    void rejectsSameKeyWithDifferentPayload() {
        FakeRepository repository = new FakeRepository();
        ContactSubmissionApplicationService service = service(repository);
        UUID key = UUID.randomUUID();
        service.submit(command(key, "Ada", "Message"));

        assertThatThrownBy(() -> service.submit(command(key, "Ada", "Different")))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void resolvesDuplicateKeyRaceWhenStoredHashMatches() {
        FakeRepository repository = new FakeRepository();
        UUID key = UUID.randomUUID();
        ContactSubmission existing = existingSubmission(command(key, "Ada", "Message"));
        repository.saved.put(existing.idempotencyKey(), existing);
        repository.throwDuplicateOnSave = true;
        repository.hideRowsUntilDuplicate = true;
        ContactSubmissionApplicationService racingService = service(repository);

        SubmitContactSubmissionResult result = racingService.submit(command(key, "Ada", "Message"));

        assertThat(result.created()).isFalse();
        assertThat(result.submissionId()).isEqualTo(existing.id());
        assertThat(repository.saveCalls).isEqualTo(1);
    }

    @Test
    void resolvesDuplicateKeyRaceAsConflictWhenStoredHashDiffers() {
        FakeRepository repository = new FakeRepository();
        UUID key = UUID.randomUUID();
        ContactSubmission existing = existingSubmission(command(key, "Ada", "Original"));
        repository.saved.put(existing.idempotencyKey(), existing);
        repository.throwDuplicateOnSave = true;
        repository.hideRowsUntilDuplicate = true;
        ContactSubmissionApplicationService racingService = service(repository);

        assertThatThrownBy(() -> racingService.submit(command(key, "Ada", "Different")))
                .isInstanceOf(IdempotencyConflictException.class);
        assertThat(repository.saveCalls).isEqualTo(1);
    }

    @Test
    void rethrowsDuplicateKeyWhenNoIdempotencyRowAppearsAfterRace() {
        FakeRepository repository = new FakeRepository();
        repository.throwDuplicateOnSave = true;
        ContactSubmissionApplicationService service = service(repository);

        assertThatThrownBy(() -> service.submit(command("Ada", "Message")))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void rejectsNameThatBecomesBlankAfterNormalizationBeforeRepositoryAccess() {
        FakeRepository repository = new FakeRepository();
        ContactSubmissionApplicationService service = service(repository);

        assertThatThrownBy(() -> service.submit(command("\u00a0\u00a0", "Message")))
                .isInstanceOf(ContactSubmissionValidationException.class);
        assertThat(repository.saveCalls).isZero();
        assertThat(repository.findByIdempotencyKeyCalls).isZero();
    }

    @Test
    void rejectsMessageThatBecomesBlankAfterNormalizationBeforeRepositoryAccess() {
        FakeRepository repository = new FakeRepository();
        ContactSubmissionApplicationService service = service(repository);

        assertThatThrownBy(() -> service.submit(command("Ada", "\u2007\u2007")))
                .isInstanceOf(ContactSubmissionValidationException.class);
        assertThat(repository.saveCalls).isZero();
        assertThat(repository.findByIdempotencyKeyCalls).isZero();
    }

    @Test
    void rejectsEmailInvalidAfterNormalizationBeforeRepositoryAccess() {
        FakeRepository repository = new FakeRepository();
        ContactSubmissionApplicationService service = service(repository);

        assertThatThrownBy(() -> service.submit(commandWithEmail("  invalid-email  ")))
                .isInstanceOf(ContactSubmissionValidationException.class);
        assertThat(repository.saveCalls).isZero();
        assertThat(repository.findByIdempotencyKeyCalls).isZero();
    }

    @Test
    void rejectsLengthInvalidAfterNormalizationBeforeSaving() {
        FakeRepository repository = new FakeRepository();
        ContactSubmissionApplicationService service = service(repository);

        assertThatThrownBy(() -> service.submit(command(" " + "A".repeat(121) + " ", "Message")))
                .isInstanceOf(ContactSubmissionValidationException.class);
        assertThat(repository.saveCalls).isZero();
        assertThat(repository.findByIdempotencyKeyCalls).isZero();
    }

    @Test
    void acceptsEmailWithExteriorWhitespaceAfterNormalizationAndPreservesCase() {
        FakeRepository repository = new FakeRepository();
        ContactSubmissionApplicationService service = service(repository);

        service.submit(commandWithEmail("  Ada.Lovelace+Test@Example.COM  "));

        ContactSubmission saved = repository.saved.values().iterator().next();
        assertThat(saved.email()).isEqualTo("Ada.Lovelace+Test@Example.COM");
        assertThat(repository.saveCalls).isEqualTo(1);
    }

    private ContactSubmissionApplicationService service(FakeRepository repository) {
        return new ContactSubmissionApplicationService(repository, normalizer, hasher, VALIDATOR, clock);
    }

    private ContactSubmission existingSubmission(SubmitContactSubmissionCommand command) {
        NormalizedContactSubmissionPayload payload = normalizer.normalize(command);
        Instant timestamp = CLOCK_INSTANT.truncatedTo(ChronoUnit.MICROS);
        return new ContactSubmission(
                UUID.randomUUID(),
                command.idempotencyKey(),
                hasher.hash(payload),
                payload.source(),
                payload.locale(),
                payload.name(),
                payload.email(),
                payload.phone(),
                payload.companyOrProject(),
                payload.message(),
                ContactStatus.RECEIVED,
                timestamp,
                timestamp);
    }

    private SubmitContactSubmissionCommand command(String name, String message) {
        return command(UUID.randomUUID(), name, message);
    }

    private SubmitContactSubmissionCommand command(UUID idempotencyKey, String name, String message) {
        return new SubmitContactSubmissionCommand(
                idempotencyKey,
                ContactSource.HOME,
                ContactLocale.ES,
                name,
                "ada@example.test",
                null,
                null,
                message);
    }

    private SubmitContactSubmissionCommand commandWithEmail(String email) {
        return new SubmitContactSubmissionCommand(
                UUID.randomUUID(),
                ContactSource.HOME,
                ContactLocale.ES,
                "Ada",
                email,
                null,
                null,
                "Message");
    }

    private static class FakeRepository implements ContactSubmissionRepository {

        private final Map<UUID, ContactSubmission> saved = new LinkedHashMap<>();
        private boolean throwDuplicateOnSave;
        private boolean hideRowsUntilDuplicate;
        private boolean duplicateThrown;
        private int saveCalls;
        private int findByIdempotencyKeyCalls;

        @Override
        public ContactSubmission save(ContactSubmission submission) {
            saveCalls++;
            if (throwDuplicateOnSave) {
                duplicateThrown = true;
                throw new DuplicateKeyException("duplicate");
            }
            saved.put(submission.idempotencyKey(), submission);
            return submission;
        }

        @Override
        public Optional<ContactSubmission> findById(UUID id) {
            return saved.values().stream().filter(submission -> submission.id().equals(id)).findFirst();
        }

        @Override
        public Optional<ContactSubmission> findByIdempotencyKey(UUID idempotencyKey) {
            findByIdempotencyKeyCalls++;
            if (hideRowsUntilDuplicate && !duplicateThrown) {
                return Optional.empty();
            }
            return Optional.ofNullable(saved.get(idempotencyKey));
        }
    }
}
