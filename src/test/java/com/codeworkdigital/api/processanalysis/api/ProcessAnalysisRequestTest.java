package com.codeworkdigital.api.processanalysis.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProcessAnalysisRequestTest {

    @Test
    void toStringRedactsDescription() {
        ProcessAnalysisRequest request = new ProcessAnalysisRequest(
                "This is a private process description.",
                "ES");

        assertThat(request.toString()).isEqualTo("ProcessAnalysisRequest[description=<redacted>, locale=ES]");
        assertThat(request.toString()).doesNotContain("private process description");
    }
}
