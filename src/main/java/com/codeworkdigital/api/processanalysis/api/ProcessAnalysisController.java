package com.codeworkdigital.api.processanalysis.api;

import com.codeworkdigital.api.processanalysis.application.AnalyzeProcessDescriptionCommand;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisApplicationService;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisLocale;
import com.codeworkdigital.api.processanalysis.application.ProcessUnderstanding;
import jakarta.validation.Valid;
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

    public ProcessAnalysisController(ProcessAnalysisApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProcessUnderstanding> analyze(@Valid @RequestBody ProcessAnalysisRequest request) {
        ProcessUnderstanding understanding = applicationService.analyze(new AnalyzeProcessDescriptionCommand(
                normalize(request.description()),
                mapLocale(normalize(request.locale()))));
        return ResponseEntity.ok(understanding);
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
