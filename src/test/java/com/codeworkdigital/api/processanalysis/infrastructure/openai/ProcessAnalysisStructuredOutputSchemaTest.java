package com.codeworkdigital.api.processanalysis.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeworkdigital.api.processanalysis.application.ProcessUnderstandingConstraints;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class ProcessAnalysisStructuredOutputSchemaTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void schemaMatchesDomainConstraints() {
        JsonNode schema = ProcessAnalysisStructuredOutputSchema.parse(objectMapper);

        assertThat(textValues(schema.at("/properties/analysisStatus/enum")))
                .containsExactly("PROCESS_IDENTIFIED", "INSUFFICIENT_INFORMATION", "OUT_OF_SCOPE");
        assertThat(textValues(schema.at("/properties/stages/items/properties/operationType/enum")))
                .containsExactly(
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
                        "OTHER");
        assertThat(schema.at("/properties/observations/maxItems").intValue())
                .isEqualTo(ProcessUnderstandingConstraints.MAX_OBSERVATIONS);
        assertThat(schema.at("/properties/observations/items/maxLength").intValue())
                .isEqualTo(ProcessUnderstandingConstraints.MAX_LIST_ITEM_LENGTH);
        assertThat(schema.at("/properties/inferences/maxItems").intValue())
                .isEqualTo(ProcessUnderstandingConstraints.MAX_INFERENCES);
        assertThat(schema.at("/properties/inferences/items/maxLength").intValue())
                .isEqualTo(ProcessUnderstandingConstraints.MAX_LIST_ITEM_LENGTH);
        assertThat(schema.at("/properties/validationQuestions/maxItems").intValue())
                .isEqualTo(ProcessUnderstandingConstraints.MAX_VALIDATION_QUESTIONS);
        assertThat(schema.at("/properties/validationQuestions/items/maxLength").intValue())
                .isEqualTo(ProcessUnderstandingConstraints.MAX_LIST_ITEM_LENGTH);
        assertThat(schema.at("/properties/stages/minItems").intValue())
                .isZero();
        assertThat(schema.at("/properties/stages/maxItems").intValue())
                .isEqualTo(ProcessUnderstandingConstraints.MAX_STAGES);
        assertThat(schema.at("/properties/stages/items/properties/id/maxLength").intValue())
                .isEqualTo(ProcessUnderstandingConstraints.MAX_STAGE_ID_LENGTH);
        assertThat(schema.at("/properties/stages/items/properties/id/pattern").textValue())
                .isEqualTo(ProcessUnderstandingConstraints.STAGE_ID_REGEX);
        assertThat(schema.at("/properties/stages/items/properties/title/maxLength").intValue())
                .isEqualTo(ProcessUnderstandingConstraints.MAX_TITLE_LENGTH);
        assertThat(schema.at("/properties/stages/items/properties/description/maxLength").intValue())
                .isEqualTo(ProcessUnderstandingConstraints.MAX_STAGE_DESCRIPTION_LENGTH);
        assertThat(schema.at("/properties/preliminaryAssessment/maxLength").intValue())
                .isEqualTo(ProcessUnderstandingConstraints.MAX_PRELIMINARY_ASSESSMENT_LENGTH);
        assertThat(schema.at("/properties/effortEvidence/additionalProperties").booleanValue()).isFalse();
        assertThat(textValues(schema.at("/properties/effortEvidence/required")))
                .containsExactly("volumePerReportingPeriod", "effortPerBusinessItem");
        assertThat(textValues(schema.at("/$defs/effortEvidenceQuantity/properties/status/enum")))
                .containsExactly("EXACT", "APPROXIMATE", "RANGE", "ABSENT", "UNSUPPORTED_UNIT");
        assertThat(textValues(schema.at("/$defs/effortEvidenceQuantity/properties/reportingPeriod/enum")))
                .containsExactly("MONTH", null);
        assertThat(textValues(schema.at("/$defs/effortEvidenceQuantity/properties/effortDuration/enum")))
                .containsExactly("MINUTE", null);
        assertThat(textValues(schema.at("/$defs/effortEvidenceQuantity/required")))
                .containsExactly(
                        "status",
                        "magnitude",
                        "minMagnitude",
                        "maxMagnitude",
                        "businessItemRef",
                        "businessItemLabel",
                        "reportingPeriod",
                        "effortDuration",
                        "evidenceText",
                        "note");
        assertThat(textValues(schema.get("required")))
                .contains(
                        "analysisStatus",
                        "observations",
                        "inferences",
                        "validationQuestions",
                        "stages",
                        "preliminaryAssessment",
                        "effortEvidence");
    }

    private java.util.List<String> textValues(JsonNode node) {
        return StreamSupport.stream(node.spliterator(), false)
                .map(JsonNode::textValue)
                .toList();
    }
}
