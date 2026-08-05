package com.codeworkdigital.api.contact.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeworkdigital.api.contact.domain.ContactLocale;
import com.codeworkdigital.api.contact.domain.ContactSource;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubmitContactSubmissionCommandTest {

    @Test
    void toStringRedactsTokenIdempotencyKeyAndPersonalData() {
        UUID idempotencyKey = UUID.fromString("00000000-0000-4000-8000-000000000001");
        SubmitContactSubmissionCommand command = new SubmitContactSubmissionCommand(
                idempotencyKey,
                ContactSource.HOME,
                ContactLocale.ES,
                "Test Person",
                "person@example.test",
                "+39 123 456",
                "Test Project",
                "Test message",
                "test-turnstile-token");

        assertThat(command.toString()).isEqualTo("SubmitContactSubmissionCommand[redacted]");
        assertThat(command.toString()).doesNotContain(
                idempotencyKey.toString(),
                "test-turnstile-token",
                "Test Person",
                "person@example.test",
                "+39 123 456",
                "Test Project",
                "Test message");
    }
}
