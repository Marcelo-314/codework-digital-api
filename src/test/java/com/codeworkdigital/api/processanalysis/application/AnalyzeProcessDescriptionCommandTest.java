package com.codeworkdigital.api.processanalysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnalyzeProcessDescriptionCommandTest {

    @Test
    void toStringRedactsDescription() {
        AnalyzeProcessDescriptionCommand command = new AnalyzeProcessDescriptionCommand(
                "Internal process description",
                ProcessAnalysisLocale.EN);

        assertThat(command.toString()).isEqualTo("AnalyzeProcessDescriptionCommand[redacted]");
        assertThat(command.toString()).doesNotContain("Internal process description");
    }
}
