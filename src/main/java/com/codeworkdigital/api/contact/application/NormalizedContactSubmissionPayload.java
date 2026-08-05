package com.codeworkdigital.api.contact.application;

import com.codeworkdigital.api.contact.domain.ContactLocale;
import com.codeworkdigital.api.contact.domain.ContactSource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NormalizedContactSubmissionPayload(
        @NotNull ContactSource source,
        @NotNull ContactLocale locale,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 254) @Email String email,
        @Size(max = 40) String phone,
        @Size(max = 160) String companyOrProject,
        @NotBlank @Size(max = 4000) String message) {
}
