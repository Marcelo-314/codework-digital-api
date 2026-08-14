package com.codeworkdigital.api.processanalysis.infrastructure.openai;

import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisLocale;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import tools.jackson.databind.ObjectMapper;

final class ProcessAnalysisEvalCaseLoader {

    static final String RESOURCE_PATH = "/process-analysis/eval-cases.json";

    private final ObjectMapper objectMapper;

    ProcessAnalysisEvalCaseLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    List<ProcessAnalysisEvalCase> load() {
        try (InputStream inputStream = ProcessAnalysisEvalCaseLoader.class.getResourceAsStream(RESOURCE_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing process analysis eval cases resource " + RESOURCE_PATH);
            }

            StoredEvalCase[] storedCases = objectMapper.readValue(inputStream, StoredEvalCase[].class);
            if (storedCases.length == 0) {
                throw new IllegalStateException("No process analysis eval cases configured");
            }

            List<ProcessAnalysisEvalCase> cases = new ArrayList<>(storedCases.length);
            Set<String> ids = new HashSet<>();
            for (StoredEvalCase storedCase : storedCases) {
                ProcessAnalysisEvalCase evalCase = map(storedCase);
                if (!ids.add(evalCase.id())) {
                    throw new IllegalStateException("Duplicate process analysis eval case id " + evalCase.id());
                }
                cases.add(evalCase);
            }
            return List.copyOf(cases);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read process analysis eval cases", exception);
        }
    }

    private ProcessAnalysisEvalCase map(StoredEvalCase storedCase) {
        String id = requireText(storedCase.id(), "id");
        String description = requireText(storedCase.description(), "description");
        if (description.length() > 2000) {
            throw new IllegalStateException("Eval case description exceeds command limit for " + id);
        }
        return new ProcessAnalysisEvalCase(id, mapLocale(requireText(storedCase.locale(), "locale")), description);
    }

    private ProcessAnalysisLocale mapLocale(String value) {
        return switch (value) {
            case "ES" -> ProcessAnalysisLocale.ES;
            case "EN" -> ProcessAnalysisLocale.EN;
            case "IT" -> ProcessAnalysisLocale.IT;
            default -> throw new IllegalStateException("Unsupported eval case locale " + value);
        };
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Eval case " + field + " is required");
        }
        return value.strip();
    }

    private record StoredEvalCase(
            String id,
            String locale,
            String description) {
    }
}
