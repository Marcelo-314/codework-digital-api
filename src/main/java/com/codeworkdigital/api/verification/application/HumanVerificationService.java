package com.codeworkdigital.api.verification.application;

public interface HumanVerificationService {

    void verify(String token, HumanVerificationContext context);
}
