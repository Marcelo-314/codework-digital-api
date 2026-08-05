package com.codeworkdigital.api.contact.application;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException() {
        super("Idempotency key was already used with a different payload");
    }
}
