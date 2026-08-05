package com.codeworkdigital.api.contact.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ContactSubmission(
        UUID id,
        UUID idempotencyKey,
        String payloadHash,
        ContactSource source,
        ContactLocale locale,
        String name,
        String email,
        String phone,
        String companyOrProject,
        String message,
        ContactStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public ContactSubmission {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(payloadHash, "payloadHash must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(locale, "locale must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
