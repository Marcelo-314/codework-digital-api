package com.codeworkdigital.api.processanalysis.application;

import com.codeworkdigital.api.processanalysis.domain.ProcessBusinessItemPerReportingPeriodUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessBusinessItemUnitId;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortDurationUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessEffortPerBusinessItemUnit;
import com.codeworkdigital.api.processanalysis.domain.ProcessQuantityProjection;
import com.codeworkdigital.api.processanalysis.domain.ProcessReportingPeriodUnit;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ProcessEffortEvidenceProjectionMapper {

    public ProcessAnalysisResult map(ProcessAnalysisModelResult modelResult) {
        if (!modelResult.understanding().isProcessIdentified()) {
            return new ProcessAnalysisResult(
                    modelResult.understanding(),
                    modelResult.effortEvidence(),
                    Optional.empty(),
                    Optional.empty(),
                    false);
        }

        Optional<ProcessQuantityProjection> volume =
                mapVolume(modelResult.effortEvidence().volumePerReportingPeriod());
        Optional<ProcessQuantityProjection> effort =
                mapEffort(modelResult.effortEvidence().effortPerBusinessItem());

        return new ProcessAnalysisResult(
                modelResult.understanding(),
                modelResult.effortEvidence(),
                volume,
                effort,
                isComposable(volume, effort));
    }

    private Optional<ProcessQuantityProjection> mapVolume(ProcessEffortEvidenceQuantity quantity) {
        if (quantity == null
                || quantity.status() != ProcessEffortEvidenceQuantityStatus.EXACT
                || quantity.magnitude() == null
                || isBlank(quantity.businessItemRef())
                || quantity.reportingPeriod() != ProcessReportingPeriodUnit.MONTH) {
            return Optional.empty();
        }

        try {
            return Optional.of(new ProcessQuantityProjection(
                    quantity.magnitude(),
                    new ProcessBusinessItemPerReportingPeriodUnit(
                            // Local to this single process-analysis result; never persisted or compared across analyses.
                            new ProcessBusinessItemUnitId(quantity.businessItemRef()),
                            ProcessReportingPeriodUnit.MONTH)));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private Optional<ProcessQuantityProjection> mapEffort(ProcessEffortEvidenceQuantity quantity) {
        if (quantity == null
                || quantity.status() != ProcessEffortEvidenceQuantityStatus.EXACT
                || quantity.magnitude() == null
                || isBlank(quantity.businessItemRef())
                || quantity.effortDuration() != ProcessEffortDurationUnit.MINUTE) {
            return Optional.empty();
        }

        try {
            return Optional.of(new ProcessQuantityProjection(
                    quantity.magnitude(),
                    new ProcessEffortPerBusinessItemUnit(
                            ProcessEffortDurationUnit.MINUTE,
                            // Local to this single process-analysis result; never persisted or compared across analyses.
                            new ProcessBusinessItemUnitId(quantity.businessItemRef()))));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private boolean isComposable(
            Optional<ProcessQuantityProjection> volume,
            Optional<ProcessQuantityProjection> effort) {
        if (volume.isEmpty() || effort.isEmpty()) {
            return false;
        }
        if (volume.get().unit() instanceof ProcessBusinessItemPerReportingPeriodUnit volumeUnit
                && effort.get().unit() instanceof ProcessEffortPerBusinessItemUnit effortUnit) {
            return volumeUnit.businessItemId().equals(effortUnit.businessItemId());
        }
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
