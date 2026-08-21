package com.codeworkdigital.api.processanalysis.application;

public class ProcessEffortClarificationLifecycleConflictException extends RuntimeException {

    public ProcessEffortClarificationLifecycleConflictException() {
        super("clarification continuation is no longer active");
    }
}
