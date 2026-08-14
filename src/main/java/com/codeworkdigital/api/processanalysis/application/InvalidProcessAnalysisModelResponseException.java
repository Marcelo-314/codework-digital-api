package com.codeworkdigital.api.processanalysis.application;

public class InvalidProcessAnalysisModelResponseException extends RuntimeException {

    private final String reason;

    public InvalidProcessAnalysisModelResponseException(String reason) {
        super("Process analysis model response is invalid");
        this.reason = reason;
    }

    public InvalidProcessAnalysisModelResponseException(String reason, Throwable cause) {
        super("Process analysis model response is invalid", cause);
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
