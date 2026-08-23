package com.codeworkdigital.api.processanalysis.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ProcessAnalysisLiveEvalManualTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void runsLiveEvaluationOnlyWhenExplicitlyEnabled() {
        ProcessAnalysisLiveEvalSettings settings = ProcessAnalysisLiveEvalSettings.load();
        Assumptions.assumeTrue(
                settings.enabled(),
                "Set PROCESS_ANALYSIS_LIVE_EVAL_ENABLED=true together with OPENAI_API_KEY and PROCESS_ANALYSIS_MODEL to run live evals.");

        ProcessAnalysisLiveEvalHarness.ProcessAnalysisLiveEvalRun run = new ProcessAnalysisLiveEvalHarness(
                objectMapper,
                settings,
                settings.selectCases(new ProcessAnalysisEvalCaseLoader(objectMapper).load()))
                .run();

        assertThat(run.outputDirectory()).exists();
        assertThat(run.outputDirectory().resolve("summary.md")).exists();
        assertThat(run.outputDirectory().resolve("results.json")).exists();
        assertThat(run.report().executionCount()).isEqualTo(run.report().caseCount() * settings.runs());
        System.out.println("Process analysis live eval report: " + run.outputDirectory().toAbsolutePath());
        assertThat(run.report().successCount() + run.report().failureCount())
                .as("Live eval failures are observational and captured in the generated report directory")
                .isEqualTo(run.report().executionCount());
    }
}
