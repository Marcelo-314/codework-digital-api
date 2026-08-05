package com.codeworkdigital.api.contact.application;

import jakarta.validation.ConstraintViolation;
import java.util.Set;

public class ContactSubmissionValidationException extends RuntimeException {

    private final Set<ConstraintViolation<NormalizedContactSubmissionPayload>> violations;

    public ContactSubmissionValidationException(
            Set<ConstraintViolation<NormalizedContactSubmissionPayload>> violations) {
        super("Contact submission payload validation failed");
        this.violations = Set.copyOf(violations);
    }

    public Set<ConstraintViolation<NormalizedContactSubmissionPayload>> violations() {
        return violations;
    }
}
