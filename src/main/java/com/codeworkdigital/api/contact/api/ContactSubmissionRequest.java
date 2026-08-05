package com.codeworkdigital.api.contact.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ContactSubmissionRequest(
        @NotBlank String source,
        @NotBlank String locale,
        @NotNull String name,
        @NotNull String email,
        String phone,
        String companyOrProject,
        @NotNull String message) {
}
