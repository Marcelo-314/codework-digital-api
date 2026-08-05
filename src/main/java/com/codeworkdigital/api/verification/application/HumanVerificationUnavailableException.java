package com.codeworkdigital.api.verification.application;

public class HumanVerificationUnavailableException extends RuntimeException {

    public HumanVerificationUnavailableException() {
        super("Human verification service is unavailable");
    }

    public HumanVerificationUnavailableException(Throwable cause) {
        super("Human verification service is unavailable", cause);
    }
}
