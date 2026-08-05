package com.codeworkdigital.api.contact.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ContactSubmissionRequestTest {

    @Test
    void toStringRedactsTokenAndPersonalData() {
        ContactSubmissionRequest request = new ContactSubmissionRequest(
                "HOME",
                "es",
                "Test Person",
                "person@example.test",
                "+39 123 456",
                "Test Project",
                "Test message",
                "test-turnstile-token");

        assertThat(request.toString()).isEqualTo("ContactSubmissionRequest[redacted]");
        assertThat(request.toString()).doesNotContain(
                "test-turnstile-token",
                "Test Person",
                "person@example.test",
                "+39 123 456",
                "Test Project",
                "Test message");
    }
}
