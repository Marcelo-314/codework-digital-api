package com.codeworkdigital.api.processanalysis.api;

import com.codeworkdigital.api.processanalysis.application.AnalyzeProcessDescriptionCommand;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisApplicationService;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisLocale;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisResult;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContinuationId;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationContinuationIssuer;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/labs/process-analysis")
public class ProcessAnalysisController {

    private final ProcessAnalysisApplicationService applicationService;
    private final ProcessEffortClarificationContinuationIssuer continuationIssuer;

    public ProcessAnalysisController(
            ProcessAnalysisApplicationService applicationService,
            ProcessEffortClarificationContinuationIssuer continuationIssuer) {
        this.applicationService = applicationService;
        this.continuationIssuer = continuationIssuer;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProcessAnalysisResponse> analyze(@Valid @RequestBody ProcessAnalysisRequest request) {
        ProcessAnalysisResult result = applicationService.analyze(new AnalyzeProcessDescriptionCommand(
                normalize(request.description()),
                mapLocale(normalize(request.locale()))));
        Optional<ProcessEffortClarificationContinuationId> clarificationId = continuationIssuer.issue(result);
        return ResponseEntity.ok(ProcessAnalysisResponse.from(result, clarificationId));
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
}
