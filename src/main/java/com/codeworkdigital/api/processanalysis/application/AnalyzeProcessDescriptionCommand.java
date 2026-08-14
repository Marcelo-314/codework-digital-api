package com.codeworkdigital.api.processanalysis.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnalyzeProcessDescriptionCommand(
        @NotBlank @Size(max = 2000) String description,
        @NotNull ProcessAnalysisLocale locale) {

    @Override
    public String toString() {
        return "AnalyzeProcessDescriptionCommand[redacted]";
    }
}
