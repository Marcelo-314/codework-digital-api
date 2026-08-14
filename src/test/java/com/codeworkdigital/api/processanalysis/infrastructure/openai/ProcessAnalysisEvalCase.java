package com.codeworkdigital.api.processanalysis.infrastructure.openai;

import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisLocale;

record ProcessAnalysisEvalCase(
        String id,
        ProcessAnalysisLocale locale,
        String description) {
}
