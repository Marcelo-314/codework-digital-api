package com.codeworkdigital.api.contact.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContactSubmissionRequest(
        @NotBlank String source,
        @NotBlank String locale,
        @NotNull String name,
        @NotNull String email,
        String phone,
        String companyOrProject,
        @NotNull String message,
        @NotBlank @Size(max = 2048) String turnstileToken) {

    @Override
    public String toString() {
        return "ContactSubmissionRequest[redacted]";
    }
}
