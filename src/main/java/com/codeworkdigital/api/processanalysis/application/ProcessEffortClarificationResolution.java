package com.codeworkdigital.api.processanalysis.application;

import java.util.Objects;

public record ProcessEffortClarificationResolution(
        ProcessEffortClarificationKnowledge clarificationKnowledge,
        ProcessEffortEstablishedKnowledge establishedKnowledge,
        ProcessEffortDerivedResult derivedResult,
        ProcessEffortMaterialityAssessment materialityAssessment) {

    public ProcessEffortClarificationResolution {
        clarificationKnowledge = Objects.requireNonNull(clarificationKnowledge, "clarificationKnowledge");
        establishedKnowledge = Objects.requireNonNull(establishedKnowledge, "establishedKnowledge");
        derivedResult = Objects.requireNonNull(derivedResult, "derivedResult");
        materialityAssessment = Objects.requireNonNull(materialityAssessment, "materialityAssessment");
    }
}
