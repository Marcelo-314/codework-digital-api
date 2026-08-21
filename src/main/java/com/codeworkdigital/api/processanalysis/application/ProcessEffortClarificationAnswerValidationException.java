package com.codeworkdigital.api.processanalysis.application;

public class ProcessEffortClarificationAnswerValidationException extends RuntimeException {

    public ProcessEffortClarificationAnswerValidationException() {
        super("invalid clarification answers");
    }
}
