package com.codeworkdigital.api.verification.application;

public class HumanVerificationRejectedException extends RuntimeException {

    public HumanVerificationRejectedException() {
        super("Human verification failed");
    }
}
