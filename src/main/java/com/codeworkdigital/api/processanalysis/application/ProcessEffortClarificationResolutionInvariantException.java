package com.codeworkdigital.api.processanalysis.application;

public class ProcessEffortClarificationResolutionInvariantException extends RuntimeException {

    public ProcessEffortClarificationResolutionInvariantException(Throwable cause) {
        super("clarification resolution invariant failed", cause);
    }
}
