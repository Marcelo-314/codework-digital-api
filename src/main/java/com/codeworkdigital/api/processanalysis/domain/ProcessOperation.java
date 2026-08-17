package com.codeworkdigital.api.processanalysis.domain;

import java.util.Objects;

public record ProcessOperation(String id, String title, String description) {

    public ProcessOperation {
        id = requireNonBlank(id, "id");
        title = requireNonBlank(title, "title");
        description = requireNonBlank(description, "description");
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
