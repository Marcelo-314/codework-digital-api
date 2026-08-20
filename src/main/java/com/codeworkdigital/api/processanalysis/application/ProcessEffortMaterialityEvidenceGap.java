package com.codeworkdigital.api.processanalysis.application;

import com.codeworkdigital.api.processanalysis.domain.ProcessEvidenceGap;
import java.util.Objects;

public record ProcessEffortMaterialityEvidenceGap(
        ProcessEffortMaterialityEvidenceGapKind kind,
        ProcessEvidenceGap evidenceGap) {

    public ProcessEffortMaterialityEvidenceGap {
        kind = Objects.requireNonNull(kind, "kind");
        evidenceGap = Objects.requireNonNull(evidenceGap, "evidenceGap");
    }
}
