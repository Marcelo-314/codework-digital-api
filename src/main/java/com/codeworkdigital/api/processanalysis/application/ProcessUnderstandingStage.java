package com.codeworkdigital.api.processanalysis.application;

public record ProcessUnderstandingStage(
        String id,
        String title,
        String description,
        ProcessStageProvenance provenance,
        ProcessStageOperationType operationType,
        ProcessStageInputNature inputNature) {
}
