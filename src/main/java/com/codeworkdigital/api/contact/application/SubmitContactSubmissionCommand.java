package com.codeworkdigital.api.contact.application;

import com.codeworkdigital.api.contact.domain.ContactLocale;
import com.codeworkdigital.api.contact.domain.ContactSource;
import java.util.UUID;

public record SubmitContactSubmissionCommand(
        UUID idempotencyKey,
        ContactSource source,
        ContactLocale locale,
        String name,
        String email,
        String phone,
        String companyOrProject,
        String message,
        String turnstileToken) {

    @Override
    public String toString() {
        return "SubmitContactSubmissionCommand[redacted]";
    }
}
