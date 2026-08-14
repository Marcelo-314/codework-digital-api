package com.codeworkdigital.api.processanalysis.application;

public class ProcessAnalysisUnavailableException extends RuntimeException {

    private final String reason;

    public ProcessAnalysisUnavailableException(String reason) {
        super("Process analysis is unavailable");
        this.reason = reason;
    }

    public ProcessAnalysisUnavailableException(String reason, Throwable cause) {
        super("Process analysis is unavailable", cause);
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
