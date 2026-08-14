package com.codeworkdigital.api.processanalysis.infrastructure.openai;

import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisLocale;

final class ProcessAnalysisPromptTemplate {

    private static final String PROMPT_VERSION = "process-analysis-understanding-v1";

    private ProcessAnalysisPromptTemplate() {
    }

    static String instructions(ProcessAnalysisLocale locale) {
        return """
                Prompt version: %s

                Analyze only the supplied business process description.
                Produce all human-readable fields in %s.
                Do not recommend technology, automation, AI, vendors, services, budgets, or opportunities.
                Do not invent systems, actors, approvals, rules, or exceptions as stated facts.
                Keep observations limited to information explicitly stated by the user.
                Put anything reasonably implied but not explicitly stated into inferences, never into observations.
                Use validationQuestions only for material unknowns that would block a real architecture recommendation. Do not add filler questions.
                Model stages as operational steps in the process, not implementation steps.
                If a stage is inferred instead of explicit, set provenance to INFERRED.
                Use only these provenance values: OBSERVED, INFERRED.
                Use only these operationType values: RECEIVE, INTERPRET, ENTER_DATA, LOOKUP, VALIDATE, CALCULATE, COMPARE, DECIDE, APPROVE, ROUTE, COMMUNICATE, OTHER.
                Use only these inputNature values: STRUCTURED, UNSTRUCTURED, MIXED, UNKNOWN.
                Use short stable stage ids in ASCII lowercase kebab-case.
                Keep each observation, inference, and validation question brief and self-contained.
                The preliminaryAssessment must be explicitly preliminary and mention important uncertainties when they exist.
                Never include marketing language. Never mention CodeWork Digital. Never recommend AI.
                """.formatted(PROMPT_VERSION, localeLanguage(locale));
    }

    private static String localeLanguage(ProcessAnalysisLocale locale) {
        return switch (locale) {
            case ES -> "Spanish";
            case EN -> "English";
            case IT -> "Italian";
        };
    }
}
