package com.codeworkdigital.api.contact.application;

public class InvalidAdminPaginationException extends RuntimeException {

    private final String field;

    public InvalidAdminPaginationException(String field) {
        this.field = field;
    }

    public String field() {
        return field;
    }
}
