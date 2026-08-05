package com.codeworkdigital.api.contact.domain;

import java.util.Optional;
import java.util.UUID;

public interface ContactSubmissionRepository {

    ContactSubmission save(ContactSubmission submission);

    Optional<ContactSubmission> findById(UUID id);

    Optional<ContactSubmission> findByIdempotencyKey(UUID idempotencyKey);
}
