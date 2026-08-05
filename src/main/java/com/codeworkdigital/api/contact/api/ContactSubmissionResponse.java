package com.codeworkdigital.api.contact.api;

import java.time.Instant;
import java.util.UUID;

public record ContactSubmissionResponse(UUID submissionId, String status, Instant receivedAt) {
}
