package com.codeworkdigital.api.processanalysis.api;

import com.codeworkdigital.api.processanalysis.application.ProcessEffortMaterialityEvidenceGap;
import java.util.Objects;

public record ProcessAnalysisClarificationQuestionResponse(
        String code,
        String question) {

    public ProcessAnalysisClarificationQuestionResponse {
        code = Objects.requireNonNull(code, "code");
        question = Objects.requireNonNull(question, "question");
    }

    static ProcessAnalysisClarificationQuestionResponse from(ProcessEffortMaterialityEvidenceGap gap) {
        Objects.requireNonNull(gap, "gap");
        return new ProcessAnalysisClarificationQuestionResponse(
                gap.kind().name(),
                gap.evidenceGap().question());
    }
}
