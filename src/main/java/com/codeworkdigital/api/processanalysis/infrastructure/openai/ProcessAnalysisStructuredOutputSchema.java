package com.codeworkdigital.api.processanalysis.infrastructure.openai;

import com.codeworkdigital.api.processanalysis.application.ProcessUnderstandingConstraints;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

final class ProcessAnalysisStructuredOutputSchema {

    static final String FORMAT_NAME = "process_understanding_v3";

    private static final String SCHEMA_JSON_TEMPLATE = """
            {
              "type": "object",
              "properties": {
                "analysisStatus": {
                  "type": "string",
                  "enum": [
                    "PROCESS_IDENTIFIED",
                    "INSUFFICIENT_INFORMATION",
                    "OUT_OF_SCOPE"
                  ]
                },
                "observations": {
                  "type": "array",
                  "maxItems": %d,
                  "items": {
                    "type": "string",
                    "maxLength": %d
                  }
                },
                "inferences": {
                  "type": "array",
                  "maxItems": %d,
                  "items": {
                    "type": "string",
                    "maxLength": %d
                  }
                },
                "validationQuestions": {
                  "type": "array",
                  "maxItems": %d,
                  "items": {
                    "type": "string",
                    "maxLength": %d
                  }
                },
                "stages": {
                  "type": "array",
                  "minItems": 0,
                  "maxItems": %d,
                  "items": {
                    "type": "object",
                    "properties": {
                      "id": {
                        "type": "string",
                        "maxLength": %d,
                        "pattern": "%s"
                      },
                      "title": {
                        "type": "string",
                        "maxLength": %d
                      },
                      "description": {
                        "type": "string",
                        "maxLength": %d
                      },
                      "provenance": {
                        "type": "string",
                        "enum": ["OBSERVED", "INFERRED"]
                      },
                      "operationType": {
                        "type": "string",
                        "enum": [
                          "RECEIVE",
                          "INTERPRET",
                          "CLASSIFY",
                          "ENTER_DATA",
                          "LOOKUP",
                          "VALIDATE",
                          "CALCULATE",
                          "COMPARE",
                          "DECIDE",
                          "APPROVE",
                          "ROUTE",
                          "COMMUNICATE",
                          "OTHER"
                        ]
                      },
                      "inputNature": {
                        "type": "string",
                        "enum": ["STRUCTURED", "UNSTRUCTURED", "MIXED", "UNKNOWN"]
                      }
                    },
                    "required": [
                      "id",
                      "title",
                      "description",
                      "provenance",
                      "operationType",
                      "inputNature"
                    ],
                    "additionalProperties": false
                  }
                },
                "preliminaryAssessment": {
                  "type": "string",
                  "maxLength": %d
                }
              },
              "required": [
                "analysisStatus",
                "observations",
                "inferences",
                "validationQuestions",
                "stages",
                "preliminaryAssessment"
              ],
              "additionalProperties": false
            }
            """;

    private ProcessAnalysisStructuredOutputSchema() {
    }

    static JsonNode parse(ObjectMapper objectMapper) {
        try {
            return objectMapper.readTree(SCHEMA_JSON_TEMPLATE.formatted(
                    ProcessUnderstandingConstraints.MAX_OBSERVATIONS,
                    ProcessUnderstandingConstraints.MAX_LIST_ITEM_LENGTH,
                    ProcessUnderstandingConstraints.MAX_INFERENCES,
                    ProcessUnderstandingConstraints.MAX_LIST_ITEM_LENGTH,
                    ProcessUnderstandingConstraints.MAX_VALIDATION_QUESTIONS,
                    ProcessUnderstandingConstraints.MAX_LIST_ITEM_LENGTH,
                    ProcessUnderstandingConstraints.MAX_STAGES,
                    ProcessUnderstandingConstraints.MAX_STAGE_ID_LENGTH,
                    ProcessUnderstandingConstraints.STAGE_ID_REGEX,
                    ProcessUnderstandingConstraints.MAX_TITLE_LENGTH,
                    ProcessUnderstandingConstraints.MAX_STAGE_DESCRIPTION_LENGTH,
                    ProcessUnderstandingConstraints.MAX_PRELIMINARY_ASSESSMENT_LENGTH));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not parse process analysis output schema", exception);
        }
    }
}
