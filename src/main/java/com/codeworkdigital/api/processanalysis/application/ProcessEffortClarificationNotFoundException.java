package com.codeworkdigital.api.processanalysis.application;

public class ProcessEffortClarificationNotFoundException extends RuntimeException {

    public ProcessEffortClarificationNotFoundException() {
        super("clarification continuation not found");
    }
}
