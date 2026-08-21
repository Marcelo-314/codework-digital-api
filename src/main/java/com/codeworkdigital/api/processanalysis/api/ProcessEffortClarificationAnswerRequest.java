package com.codeworkdigital.api.processanalysis.api;

import com.codeworkdigital.api.processanalysis.application.ProcessEffortMaterialityClarificationAnswer;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortMaterialityEvidenceGapKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ProcessEffortClarificationAnswerRequest(
        @NotNull @NotEmpty @Size(max = 2) List<@NotNull @Valid Answer> answers) {

    List<ProcessEffortMaterialityClarificationAnswer> toApplicationAnswers() {
        return answers.stream()
                .map(answer -> new ProcessEffortMaterialityClarificationAnswer(
                        mapAnswerCode(normalize(answer.code())),
                        answer.value()))
                .toList();
    }

    private static String normalize(String value) {
        return value == null ? null : value.strip();
    }

    private static ProcessEffortMaterialityEvidenceGapKind mapAnswerCode(String code) {
        return switch (code) {
            case "VOLUME_PER_REPORTING_PERIOD" ->
                    ProcessEffortMaterialityEvidenceGapKind.VOLUME_PER_REPORTING_PERIOD;
            case "EFFORT_PER_BUSINESS_ITEM" ->
                    ProcessEffortMaterialityEvidenceGapKind.EFFORT_PER_BUSINESS_ITEM;
            default -> throw new UnsupportedProcessAnalysisValueException("answers.code");
        };
    }

    public record Answer(
            @NotBlank String code,
            @NotNull @DecimalMin("0") BigDecimal value) {
    }
}
