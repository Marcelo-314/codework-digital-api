package com.codeworkdigital.api.contact.api;

public class UnsupportedContactValueException extends RuntimeException {

    private final String field;

    public UnsupportedContactValueException(String field) {
        super("Unsupported contact request value");
        this.field = field;
    }

    public String field() {
        return field;
    }
}
