package com.codeworkdigital.api.processanalysis.api;

public class UnsupportedProcessAnalysisValueException extends RuntimeException {

    private final String field;

    public UnsupportedProcessAnalysisValueException(String field) {
        super("Unsupported process analysis value");
        this.field = field;
    }

    public String field() {
        return field;
    }
}
