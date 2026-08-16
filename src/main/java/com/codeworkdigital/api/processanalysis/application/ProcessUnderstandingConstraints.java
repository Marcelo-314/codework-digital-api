package com.codeworkdigital.api.processanalysis.application;

public final class ProcessUnderstandingConstraints {

    public static final int MAX_OBSERVATIONS = 12;
    public static final int MAX_INFERENCES = 12;
    public static final int MAX_VALIDATION_QUESTIONS = 12;
    public static final int MIN_STAGES = 1;
    public static final int MAX_STAGES = 12;

    public static final int MAX_STAGE_ID_LENGTH = 48;
    public static final int MAX_TITLE_LENGTH = 120;
    public static final int MAX_STAGE_DESCRIPTION_LENGTH = 320;
    public static final int MAX_LIST_ITEM_LENGTH = 240;
    public static final int MAX_PRELIMINARY_ASSESSMENT_LENGTH = 800;

    public static final String STAGE_ID_REGEX = "^[a-z0-9]+(?:-[a-z0-9]+)*$";

    private ProcessUnderstandingConstraints() {
    }
}
