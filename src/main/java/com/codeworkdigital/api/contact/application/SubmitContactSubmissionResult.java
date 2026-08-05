package com.codeworkdigital.api.contact.application;

import com.codeworkdigital.api.contact.domain.ContactStatus;
import java.time.Instant;
import java.util.UUID;

public record SubmitContactSubmissionResult(
        UUID submissionId,
        ContactStatus status,
        Instant receivedAt,
        boolean created) {
}
