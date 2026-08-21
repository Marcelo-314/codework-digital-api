package com.codeworkdigital.api.processanalysis.application;

public class ProcessEffortClarificationExpiredException extends RuntimeException {

    public ProcessEffortClarificationExpiredException() {
        super("clarification continuation expired");
    }
}
