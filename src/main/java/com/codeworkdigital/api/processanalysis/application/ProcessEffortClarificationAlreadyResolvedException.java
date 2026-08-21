package com.codeworkdigital.api.processanalysis.application;

public class ProcessEffortClarificationAlreadyResolvedException extends RuntimeException {

    public ProcessEffortClarificationAlreadyResolvedException() {
        super("clarification continuation already resolved");
    }
}
