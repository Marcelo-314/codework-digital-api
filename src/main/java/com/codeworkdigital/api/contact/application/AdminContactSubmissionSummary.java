package com.codeworkdigital.api.contact.application;

import java.time.Instant;
import java.util.UUID;

public record AdminContactSubmissionSummary(
        UUID id,
        String source,
        String locale,
        String name,
        String email,
        String phone,
        String companyOrProject,
        String message,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
