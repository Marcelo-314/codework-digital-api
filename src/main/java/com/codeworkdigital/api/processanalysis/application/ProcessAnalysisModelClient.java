package com.codeworkdigital.api.processanalysis.application;

public interface ProcessAnalysisModelClient {

    ProcessAnalysisModelResult analyze(AnalyzeProcessDescriptionCommand command);
}
