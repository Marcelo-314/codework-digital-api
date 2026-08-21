package com.codeworkdigital.api.processanalysis.api;

import com.codeworkdigital.api.processanalysis.application.AnalyzeProcessDescriptionCommand;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisApplicationService;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisLocale;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisResult;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContinuationResolutionService;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContinuationId;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContinuationIssuer;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationResolution;
import jakarta.validation.Valid;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/labs/process-analysis")
public class ProcessAnalysisController {

    private final ProcessAnalysisApplicationService applicationService;
    private final ProcessEffortClarificationContinuationIssuer continuationIssuer;
    private final ProcessEffortClarificationContinuationResolutionService continuationResolutionService;

    public ProcessAnalysisController(
            ProcessAnalysisApplicationService applicationService,
            ProcessEffortClarificationContinuationIssuer continuationIssuer,
            ProcessEffortClarificationContinuationResolutionService continuationResolutionService) {
        this.applicationService = applicationService;
        this.continuationIssuer = continuationIssuer;
        this.continuationResolutionService = continuationResolutionService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProcessAnalysisResponse> analyze(@Valid @RequestBody ProcessAnalysisRequest request) {
        ProcessAnalysisResult result = applicationService.analyze(new AnalyzeProcessDescriptionCommand(
                normalize(request.description()),
                mapLocale(normalize(request.locale()))));
        Optional<ProcessEffortClarificationContinuationId> clarificationId = continuationIssuer.issue(result);
        return ResponseEntity.ok(ProcessAnalysisResponse.from(result, clarificationId));
    }

    @PostMapping(
            path = "/clarifications/{clarificationId}/answers",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProcessEffortClarificationResolutionResponse> answerClarification(
            @PathVariable String clarificationId,
            @Valid @RequestBody ProcessEffortClarificationAnswerRequest request) {
        ProcessEffortClarificationContinuationId id = parseClarificationId(clarificationId);
        ProcessEffortClarificationResolution resolution = continuationResolutionService.resolve(
                id,
                request.toApplicationAnswers());
        return ResponseEntity.ok(ProcessEffortClarificationResolutionResponse.from(id, resolution));
    }

    private String normalize(String value) {
        return value == null ? null : value.strip();
    }

    private ProcessAnalysisLocale mapLocale(String value) {
        return switch (value) {
            case "ES" -> ProcessAnalysisLocale.ES;
            case "EN" -> ProcessAnalysisLocale.EN;
            case "IT" -> ProcessAnalysisLocale.IT;
            default -> throw new UnsupportedProcessAnalysisValueException("locale");
        };
    }

    private ProcessEffortClarificationContinuationId parseClarificationId(String value) {
        try {
            return new ProcessEffortClarificationContinuationId(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            throw new UnsupportedProcessAnalysisValueException("clarificationId");
        }
    }
}
