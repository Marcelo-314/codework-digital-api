package com.codeworkdigital.api.processanalysis.application;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class ProcessUnderstandingValidator {

    private static final Pattern STAGE_ID_PATTERN = Pattern.compile(ProcessUnderstandingConstraints.STAGE_ID_REGEX);

    private ProcessUnderstandingValidator() {
    }

    public static void validate(ProcessUnderstandingDraft draft) {
        if (draft == null) {
            throw new InvalidProcessAnalysisModelResponseException("missing_process_understanding");
        }

        validateList("observations", draft.observations(), ProcessUnderstandingConstraints.MAX_OBSERVATIONS);
        validateList("inferences", draft.inferences(), ProcessUnderstandingConstraints.MAX_INFERENCES);
        validateList(
                "validationQuestions",
                draft.validationQuestions(),
                ProcessUnderstandingConstraints.MAX_VALIDATION_QUESTIONS);
        validateStages(draft.stages());
        validateText(
                draft.preliminaryAssessment(),
                "preliminaryAssessment",
                ProcessUnderstandingConstraints.MAX_PRELIMINARY_ASSESSMENT_LENGTH);
    }

    private static void validateList(String field, List<String> items, int maxSize) {
        if (items == null) {
            throw new InvalidProcessAnalysisModelResponseException(field + "_missing");
        }
        if (items.size() > maxSize) {
            throw new InvalidProcessAnalysisModelResponseException(field + "_too_large");
        }
        for (String item : items) {
            validateText(item, field + "Item", ProcessUnderstandingConstraints.MAX_LIST_ITEM_LENGTH);
        }
    }

    private static void validateStages(List<ProcessUnderstandingStage> stages) {
        if (stages == null) {
            throw new InvalidProcessAnalysisModelResponseException("stages_missing");
        }
        if (stages.isEmpty()) {
            throw new InvalidProcessAnalysisModelResponseException("stages_empty");
        }
        if (stages.size() > ProcessUnderstandingConstraints.MAX_STAGES) {
            throw new InvalidProcessAnalysisModelResponseException("stages_too_large");
        }

        Set<String> ids = new HashSet<>();
        for (ProcessUnderstandingStage stage : stages) {
            if (stage == null) {
                throw new InvalidProcessAnalysisModelResponseException("stage_null");
            }
            validateText(stage.id(), "stage.id", ProcessUnderstandingConstraints.MAX_STAGE_ID_LENGTH);
            if (!STAGE_ID_PATTERN.matcher(stage.id()).matches()) {
                throw new InvalidProcessAnalysisModelResponseException("stage_id_invalid");
            }
            if (!ids.add(stage.id())) {
                throw new InvalidProcessAnalysisModelResponseException("stage_id_duplicate");
            }
            validateText(stage.title(), "stage.title", ProcessUnderstandingConstraints.MAX_TITLE_LENGTH);
            validateText(
                    stage.description(),
                    "stage.description",
                    ProcessUnderstandingConstraints.MAX_STAGE_DESCRIPTION_LENGTH);
            if (stage.provenance() == null) {
                throw new InvalidProcessAnalysisModelResponseException("stage_provenance_missing");
            }
            if (stage.operationType() == null) {
                throw new InvalidProcessAnalysisModelResponseException("stage_operation_type_missing");
            }
            if (stage.inputNature() == null) {
                throw new InvalidProcessAnalysisModelResponseException("stage_input_nature_missing");
            }
        }
    }

    private static void validateText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new InvalidProcessAnalysisModelResponseException(field + "_blank");
        }
        if (value.length() > maxLength) {
            throw new InvalidProcessAnalysisModelResponseException(field + "_too_long");
        }
    }
}
