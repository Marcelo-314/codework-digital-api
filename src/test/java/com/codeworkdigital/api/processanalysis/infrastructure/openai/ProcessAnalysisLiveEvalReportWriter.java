package com.codeworkdigital.api.processanalysis.infrastructure.openai;

import com.codeworkdigital.api.processanalysis.application.ProcessUnderstandingStage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import tools.jackson.databind.ObjectMapper;

final class ProcessAnalysisLiveEvalReportWriter {

    private final ObjectMapper objectMapper;

    ProcessAnalysisLiveEvalReportWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    Path write(
            ProcessAnalysisLiveEvalHarness.ProcessAnalysisLiveEvalReport report,
            Path outputRoot) {
        Path runDirectory = outputRoot.resolve(report.runId());
        try {
            Files.createDirectories(runDirectory);
            Files.writeString(runDirectory.resolve("summary.md"), buildSummary(report));
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(runDirectory.resolve("results.json").toFile(), report);
            return runDirectory;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write process analysis live eval report", exception);
        }
    }

    private String buildSummary(ProcessAnalysisLiveEvalHarness.ProcessAnalysisLiveEvalReport report) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Process Analysis Live Evaluation\n\n");
        markdown.append("This report is observational only. It does not assign a semantic score.\n\n");
        markdown.append("- Generated at: `").append(report.generatedAt()).append("`\n");
        markdown.append("- Model: `").append(report.model()).append("`\n");
        markdown.append("- Cases: ").append(report.caseCount()).append("\n");
        markdown.append("- Runs per case: ").append(report.runsPerCase()).append("\n");
        markdown.append("- Total executions: ").append(report.executionCount()).append("\n");
        markdown.append("- Successes: ").append(report.successCount()).append("\n");
        markdown.append("- Failures: ").append(report.failureCount()).append("\n\n");

        markdown.append("## Latency\n\n");
        markdown.append("- min: ").append(report.latency().minMillis()).append(" ms\n");
        markdown.append("- p50: ").append(report.latency().p50Millis()).append(" ms\n");
        markdown.append("- max: ").append(report.latency().maxMillis()).append(" ms\n");
        if (report.latency().p95Millis() != null) {
            markdown.append("- p95: ").append(report.latency().p95Millis()).append(" ms\n");
        }
        markdown.append("\n");

        markdown.append("## Review Checklist\n\n");
        for (String item : report.reviewChecklist()) {
            markdown.append("- ").append(item).append("\n");
        }
        markdown.append("\n");

        Map<String, List<ProcessAnalysisLiveEvalHarness.ProcessAnalysisLiveEvalExecution>> byCase =
                report.executions().stream().collect(Collectors.groupingBy(
                        ProcessAnalysisLiveEvalHarness.ProcessAnalysisLiveEvalExecution::caseId,
                        LinkedHashMap::new,
                        Collectors.toList()));

        markdown.append("## Results\n");
        for (List<ProcessAnalysisLiveEvalHarness.ProcessAnalysisLiveEvalExecution> caseExecutions : byCase.values()) {
            appendCase(markdown, caseExecutions);
        }
        return markdown.toString();
    }

    private void appendCase(
            StringBuilder markdown,
            List<ProcessAnalysisLiveEvalHarness.ProcessAnalysisLiveEvalExecution> caseExecutions) {
        ProcessAnalysisLiveEvalHarness.ProcessAnalysisLiveEvalExecution first = caseExecutions.get(0);
        markdown.append("\n### ").append(first.caseId()).append(" (").append(first.locale()).append(")\n\n");
        markdown.append("Input:\n\n> ").append(first.description()).append("\n\n");
        appendP06StabilitySummary(markdown, caseExecutions);

        for (ProcessAnalysisLiveEvalHarness.ProcessAnalysisLiveEvalExecution execution : caseExecutions) {
            markdown.append("#### Run ").append(execution.runIndex()).append("\n\n");
            markdown.append("- Status: ").append(execution.success() ? "success" : "failure").append("\n");
            markdown.append("- Elapsed: ").append(execution.elapsedMillis()).append(" ms\n");
            markdown.append("- Analysis status: ")
                    .append(execution.analysisStatus() == null ? "n/a" : execution.analysisStatus())
                    .append("\n");
            markdown.append("- Stage count: ").append(execution.stageCount()).append("\n");
            markdown.append("- Observation count: ").append(execution.observationCount()).append("\n");
            markdown.append("- Inference count: ").append(execution.inferenceCount()).append("\n");
            markdown.append("- Validation question count: ").append(execution.validationQuestionCount()).append("\n");
            markdown.append("- Operation types: ").append(joinOrNone(execution.stageOperationTypes())).append("\n");

            if (!execution.success()) {
                markdown.append("- Error category: ").append(execution.errorCategory()).append("\n");
                markdown.append("- Error reason: ").append(execution.errorReason()).append("\n");
                markdown.append("- Error message: ").append(execution.errorMessage()).append("\n\n");
                continue;
            }

            appendP06Diagnostics(markdown, execution.p06Diagnostic());

            markdown.append("\nObservations:\n");
            appendTextList(markdown, execution.observations());

            markdown.append("\nInferences:\n");
            appendTextList(markdown, execution.inferences());

            markdown.append("\nValidation questions:\n");
            appendTextList(markdown, execution.validationQuestions());

            markdown.append("\nStages:\n");
            appendStages(markdown, execution.stages());

            markdown.append("\nPreliminary assessment:\n\n");
            markdown.append(execution.preliminaryAssessment()).append("\n\n");
        }
    }

    private void appendP06StabilitySummary(
            StringBuilder markdown,
            List<ProcessAnalysisLiveEvalHarness.ProcessAnalysisLiveEvalExecution> caseExecutions) {
        Map<String, List<ProcessAnalysisLiveEvalHarness.ProcessAnalysisLiveEvalExecution>> bySignature =
                caseExecutions.stream()
                        .filter(ProcessAnalysisLiveEvalHarness.ProcessAnalysisLiveEvalExecution::success)
                        .filter(execution -> execution.p06Diagnostic() != null)
                        .collect(Collectors.groupingBy(
                                execution -> execution.p06Diagnostic().signature(),
                                LinkedHashMap::new,
                                Collectors.toList()));
        if (bySignature.isEmpty()) {
            return;
        }

        int successfulRuns = bySignature.values().stream().mapToInt(List::size).sum();
        markdown.append("P06 evidence signatures:\n\n");
        for (Map.Entry<String, List<ProcessAnalysisLiveEvalHarness.ProcessAnalysisLiveEvalExecution>> entry
                : bySignature.entrySet()) {
            ProcessAnalysisLiveEvalHarness.P06DiagnosticFingerprint diagnostic =
                    entry.getValue().get(0).p06Diagnostic();
            markdown.append(entry.getValue().size()).append("/").append(successfulRuns).append("\n");
            markdown.append("- volume: ").append(diagnostic.evidence().volume().signature()).append("\n");
            markdown.append("- effort: ").append(diagnostic.evidence().effort().signature()).append("\n");
            markdown.append("- same nonblank business item ref: ")
                    .append(yesNo(diagnostic.evidence().sameNonblankBusinessItemRef())).append("\n");
            markdown.append("- volume projection: ")
                    .append(presentAbsent(diagnostic.admission().volumeProjectionPresent())).append("\n");
            markdown.append("- effort projection: ")
                    .append(presentAbsent(diagnostic.admission().effortProjectionPresent())).append("\n");
            markdown.append("- gaps: ").append(joinOrNone(diagnostic.admission().materialityEvidenceGapKinds())).append("\n\n");
        }
    }

    private void appendP06Diagnostics(
            StringBuilder markdown,
            ProcessAnalysisLiveEvalHarness.P06DiagnosticFingerprint diagnostic) {
        if (diagnostic == null) {
            return;
        }

        markdown.append("\nP06 evidence\n\n");
        markdown.append("Volume:\n");
        appendQuantity(markdown, diagnostic.evidence().volume());
        markdown.append("\nEffort:\n");
        appendQuantity(markdown, diagnostic.evidence().effort());
        markdown.append("\nRelationship:\n");
        markdown.append("- same nonblank business item ref: ")
                .append(yesNo(diagnostic.evidence().sameNonblankBusinessItemRef())).append("\n");

        markdown.append("\nP06 admission:\n");
        markdown.append("- volume projection: ")
                .append(presentAbsent(diagnostic.admission().volumeProjectionPresent())).append("\n");
        markdown.append("- effort projection: ")
                .append(presentAbsent(diagnostic.admission().effortProjectionPresent())).append("\n");
        markdown.append("- derived result: ")
                .append(presentAbsent(diagnostic.admission().derivedResultPresent())).append("\n");
        markdown.append("- materiality: ")
                .append(ProcessAnalysisLiveEvalHarness.valueOrNa(diagnostic.admission().materialityAssessmentStatus()))
                .append("\n");
        markdown.append("- gaps: ").append(joinOrNone(diagnostic.admission().materialityEvidenceGapKinds())).append("\n");
        markdown.append("- composable: ").append(diagnostic.admission().composable()).append("\n");
    }

    private void appendQuantity(
            StringBuilder markdown,
            ProcessAnalysisLiveEvalHarness.P06EvidenceQuantityFingerprint quantity) {
        markdown.append("- status: ").append(ProcessAnalysisLiveEvalHarness.valueOrNa(quantity.status())).append("\n");
        markdown.append("- magnitude: ").append(ProcessAnalysisLiveEvalHarness.valueOrNa(quantity.magnitude())).append("\n");
        markdown.append("- min: ").append(ProcessAnalysisLiveEvalHarness.valueOrNa(quantity.minMagnitude())).append("\n");
        markdown.append("- max: ").append(ProcessAnalysisLiveEvalHarness.valueOrNa(quantity.maxMagnitude())).append("\n");
        markdown.append("- reporting period: ")
                .append(ProcessAnalysisLiveEvalHarness.valueOrNa(quantity.reportingPeriod())).append("\n");
        markdown.append("- effort duration: ")
                .append(ProcessAnalysisLiveEvalHarness.valueOrNa(quantity.effortDuration())).append("\n");
        markdown.append("- business item ref present: ").append(yesNo(quantity.businessItemRefPresent())).append("\n");
        markdown.append("- business item label present: ").append(yesNo(quantity.businessItemLabelPresent())).append("\n");
    }

    private void appendTextList(StringBuilder markdown, List<String> values) {
        if (values.isEmpty()) {
            markdown.append("- none\n");
            return;
        }
        for (String value : values) {
            markdown.append("- ").append(value).append("\n");
        }
    }

    private void appendStages(StringBuilder markdown, List<ProcessUnderstandingStage> stages) {
        if (stages.isEmpty()) {
            markdown.append("- none\n");
            return;
        }
        int index = 1;
        for (ProcessUnderstandingStage stage : stages) {
            markdown.append(index++)
                    .append(". `").append(stage.id()).append("` - ")
                    .append(stage.title())
                    .append(" [")
                    .append(stage.provenance().name()).append(", ")
                    .append(stage.operationType().name()).append(", ")
                    .append(stage.inputNature().name()).append("]: ")
                    .append(stage.description())
                    .append("\n");
        }
    }

    private String joinOrNone(List<String> values) {
        return values.isEmpty() ? "none" : String.join(", ", values);
    }

    private String presentAbsent(boolean present) {
        return present ? "present" : "absent";
    }

    private String yesNo(boolean value) {
        return value ? "yes" : "no";
    }
}
