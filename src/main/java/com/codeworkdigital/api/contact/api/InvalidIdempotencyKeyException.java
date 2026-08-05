package com.codeworkdigital.api.contact.api;

public class InvalidIdempotencyKeyException extends RuntimeException {

    public InvalidIdempotencyKeyException() {
        super("Invalid Idempotency-Key header");
    }
}
