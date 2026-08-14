package com.codeworkdigital.api.processanalysis.infrastructure.openai;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

final class ProcessAnalysisStructuredOutputSchema {

    static final String FORMAT_NAME = "process_understanding_v1";

    private static final String SCHEMA_JSON = """
            {
              "type": "object",
              "properties": {
                "observations": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                },
                "inferences": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                },
                "validationQuestions": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                },
                "stages": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "id": {
                        "type": "string"
                      },
                      "title": {
                        "type": "string"
                      },
                      "description": {
                        "type": "string"
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
                  "type": "string"
                }
              },
              "required": [
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
            return objectMapper.readTree(SCHEMA_JSON);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not parse process analysis output schema", exception);
        }
    }
}
