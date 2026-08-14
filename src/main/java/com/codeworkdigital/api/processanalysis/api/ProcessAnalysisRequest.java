package com.codeworkdigital.api.processanalysis.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProcessAnalysisRequest(
        @NotNull String description,
        @NotBlank String locale) {

    @Override
    public String toString() {
        return "ProcessAnalysisRequest[description=<redacted>, locale=" + locale + "]";
    }
}
