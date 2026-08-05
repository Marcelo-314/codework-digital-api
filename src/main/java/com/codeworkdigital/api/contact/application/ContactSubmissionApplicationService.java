package com.codeworkdigital.api.contact.application;

import com.codeworkdigital.api.contact.domain.ContactStatus;
import com.codeworkdigital.api.contact.domain.ContactSubmission;
import com.codeworkdigital.api.contact.domain.ContactSubmissionRepository;
import com.codeworkdigital.api.contact.domain.ContactSource;
import com.codeworkdigital.api.verification.application.HumanVerificationContext;
import com.codeworkdigital.api.verification.application.HumanVerificationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class ContactSubmissionApplicationService {

    private final ContactSubmissionRepository repository;
    private final ContactSubmissionNormalizer normalizer;
    private final ContactSubmissionPayloadHasher hasher;
    private final Validator validator;
    private final HumanVerificationService humanVerificationService;
    private final Clock clock;

    public ContactSubmissionApplicationService(
            ContactSubmissionRepository repository,
            ContactSubmissionNormalizer normalizer,
            ContactSubmissionPayloadHasher hasher,
            Validator validator,
            HumanVerificationService humanVerificationService,
            Clock clock) {
        this.repository = repository;
        this.normalizer = normalizer;
        this.hasher = hasher;
        this.validator = validator;
        this.humanVerificationService = humanVerificationService;
        this.clock = clock;
    }

    public SubmitContactSubmissionResult submit(SubmitContactSubmissionCommand command) {
        NormalizedContactSubmissionPayload payload = normalizer.normalize(command);
        validate(payload);
        humanVerificationService.verify(command.turnstileToken(), contextFor(payload.source()));
        String payloadHash = hasher.hash(payload);

        return repository.findByIdempotencyKey(command.idempotencyKey())
                .map(existing -> resultForExisting(existing, payloadHash))
                .orElseGet(() -> create(command, payload, payloadHash));
    }

    private void validate(NormalizedContactSubmissionPayload payload) {
        Set<ConstraintViolation<NormalizedContactSubmissionPayload>> violations = validator.validate(payload);
        if (!violations.isEmpty()) {
            throw new ContactSubmissionValidationException(violations);
        }
    }

    private SubmitContactSubmissionResult create(
            SubmitContactSubmissionCommand command,
            NormalizedContactSubmissionPayload payload,
            String payloadHash) {
        Instant timestamp = clock.instant().truncatedTo(ChronoUnit.MICROS);
        ContactSubmission submission = new ContactSubmission(
                UUID.randomUUID(),
                command.idempotencyKey(),
                payloadHash,
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
        try {
            return resultForCreated(repository.save(submission));
        } catch (DuplicateKeyException exception) {
            return repository.findByIdempotencyKey(command.idempotencyKey())
                    .map(existing -> resultForExisting(existing, payloadHash))
                    .orElseThrow(() -> exception);
        }
    }

    private SubmitContactSubmissionResult resultForExisting(ContactSubmission existing, String payloadHash) {
        if (!existing.payloadHash().equals(payloadHash)) {
            throw new IdempotencyConflictException();
        }
        return new SubmitContactSubmissionResult(existing.id(), existing.status(), existing.createdAt(), false);
    }

    private SubmitContactSubmissionResult resultForCreated(ContactSubmission created) {
        return new SubmitContactSubmissionResult(created.id(), created.status(), created.createdAt(), true);
    }

    private HumanVerificationContext contextFor(ContactSource source) {
        return switch (source) {
            case HOME -> HumanVerificationContext.CONTACT_HOME;
            case CONTACT_PAGE -> HumanVerificationContext.CONTACT_PAGE;
        };
    }
}
