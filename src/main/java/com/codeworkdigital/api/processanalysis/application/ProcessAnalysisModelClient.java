package com.codeworkdigital.api.processanalysis.application;

public interface ProcessAnalysisModelClient {

    ProcessUnderstanding analyze(AnalyzeProcessDescriptionCommand command);
}
