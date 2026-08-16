package com.codeworkdigital.api.processanalysis.infrastructure.openai;

import com.codeworkdigital.api.processanalysis.application.AnalyzeProcessDescriptionCommand;
import com.codeworkdigital.api.processanalysis.application.InvalidProcessAnalysisModelResponseException;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisModelClient;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisUnavailableException;
import com.codeworkdigital.api.processanalysis.application.ProcessUnderstanding;
import com.codeworkdigital.api.processanalysis.application.ProcessUnderstandingDraft;
import com.codeworkdigital.api.processanalysis.application.ProcessUnderstandingStage;
import com.codeworkdigital.api.processanalysis.application.ProcessUnderstandingValidator;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

class OpenAiProcessAnalysisModelClient implements ProcessAnalysisModelClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiProcessAnalysisModelClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ObjectReader draftReader;
    private final ProcessAnalysisProperties properties;
    private final JsonNode outputSchema;

    OpenAiProcessAnalysisModelClient(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            ProcessAnalysisProperties properties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.draftReader = objectMapper.reader(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .forType(ProcessUnderstandingDraft.class);
        this.properties = properties;
        this.outputSchema = ProcessAnalysisStructuredOutputSchema.parse(this.objectMapper);
    }

    @Override
    public ProcessUnderstanding analyze(AnalyzeProcessDescriptionCommand command) {
        long startedAt = System.nanoTime();
        HttpRequest request = buildRequest(command);

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOGGER.warn(
                        "Process analysis inference unavailable: model={} inputLength={} status={} durationMs={}",
                        properties.model(),
                        command.description().length(),
                        response.statusCode(),
                        durationMillis(startedAt));
                throw new ProcessAnalysisUnavailableException("provider_http_" + response.statusCode());
            }

            ProcessUnderstanding understanding = parseUnderstanding(command.description(), response.body());
            LOGGER.info(
                    "Process analysis inference succeeded: model={} inputLength={} stageCount={} durationMs={}",
                    properties.model(),
                    command.description().length(),
                    understanding.stages().size(),
                    durationMillis(startedAt));
            return understanding;
        } catch (InvalidProcessAnalysisModelResponseException exception) {
            LOGGER.warn(
                    "Process analysis inference produced invalid structured output: model={} inputLength={} reason={} durationMs={}",
                    properties.model(),
                    command.description().length(),
                    exception.reason(),
                    durationMillis(startedAt));
            throw exception;
        } catch (ProcessAnalysisUnavailableException exception) {
            LOGGER.warn(
                    "Process analysis inference unavailable: model={} inputLength={} reason={} durationMs={}",
                    properties.model(),
                    command.description().length(),
                    exception.reason(),
                    durationMillis(startedAt));
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn(
                    "Process analysis inference interrupted: model={} inputLength={} durationMs={}",
                    properties.model(),
                    command.description().length(),
                    durationMillis(startedAt));
            throw new ProcessAnalysisUnavailableException("request_interrupted", exception);
        } catch (IOException exception) {
            LOGGER.warn(
                    "Process analysis inference failed with I/O error: model={} inputLength={} durationMs={}",
                    properties.model(),
                    command.description().length(),
                    durationMillis(startedAt));
            throw new ProcessAnalysisUnavailableException("request_io_failure", exception);
        }
    }

    private HttpRequest buildRequest(AnalyzeProcessDescriptionCommand command) {
        String body;
        try {
            body = objectMapper.writeValueAsString(buildRequestBody(command));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize process analysis request", exception);
        }

        return HttpRequest.newBuilder(properties.responsesUrl())
                .timeout(properties.requestTimeout())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
    }

    private ObjectNode buildRequestBody(AnalyzeProcessDescriptionCommand command) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.model());
        root.put("store", false);

        ArrayNode input = root.putArray("input");
        ObjectNode systemMessage = input.addObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", ProcessAnalysisPromptTemplate.instructions(command.locale()));

        ObjectNode userMessage = input.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", command.description());

        ObjectNode text = root.putObject("text");
        ObjectNode format = text.putObject("format");
        format.put("type", "json_schema");
        format.put("name", ProcessAnalysisStructuredOutputSchema.FORMAT_NAME);
        format.put("strict", true);
        format.set("schema", outputSchema.deepCopy());

        return root;
    }

    private ProcessUnderstanding parseUnderstanding(String description, String providerBody) {
        JsonNode root;
        try {
            root = objectMapper.readTree(providerBody);
        } catch (JacksonException exception) {
            throw new InvalidProcessAnalysisModelResponseException("provider_body_not_json", exception);
        }

        String status = textValue(root.get("status"));
        if (status != null && !"completed".equals(status)) {
            throw new InvalidProcessAnalysisModelResponseException("response_status_" + status);
        }

        String outputText = extractOutputText(root);
        ProcessUnderstandingDraft draft;
        try {
            draft = draftReader.readValue(outputText);
        } catch (JacksonException exception) {
            throw new InvalidProcessAnalysisModelResponseException("structured_output_unparseable", exception);
        }

        ProcessUnderstandingDraft sanitized = sanitize(draft);
        ProcessUnderstandingValidator.validate(sanitized);
        return new ProcessUnderstanding(
                description,
                sanitized.analysisStatus(),
                sanitized.observations(),
                sanitized.inferences(),
                sanitized.validationQuestions(),
                sanitized.stages(),
                sanitized.preliminaryAssessment());
    }

    private String extractOutputText(JsonNode root) {
        JsonNode output = root.get("output");
        if (output == null || !output.isArray()) {
            throw new InvalidProcessAnalysisModelResponseException("output_missing");
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode item : output) {
            if (!"message".equals(textValue(item.get("type")))) {
                continue;
            }
            JsonNode content = item.get("content");
            if (content == null || !content.isArray()) {
                continue;
            }
            for (JsonNode contentItem : content) {
                String type = textValue(contentItem.get("type"));
                if ("refusal".equals(type)) {
                    throw new InvalidProcessAnalysisModelResponseException("model_refusal");
                }
                if ("output_text".equals(type)) {
                    String part = textValue(contentItem.get("text"));
                    if (part == null) {
                        throw new InvalidProcessAnalysisModelResponseException("output_text_missing");
                    }
                    text.append(part);
                }
            }
        }

        if (text.isEmpty()) {
            throw new InvalidProcessAnalysisModelResponseException("output_text_absent");
        }
        return text.toString();
    }

    private ProcessUnderstandingDraft sanitize(ProcessUnderstandingDraft draft) {
        return new ProcessUnderstandingDraft(
                draft.analysisStatus(),
                sanitizeList(draft.observations()),
                sanitizeList(draft.inferences()),
                sanitizeList(draft.validationQuestions()),
                sanitizeStages(draft.stages()),
                sanitizeText(draft.preliminaryAssessment()));
    }

    private List<String> sanitizeList(List<String> items) {
        if (items == null) {
            return null;
        }
        return items.stream()
                .map(this::sanitizeText)
                .toList();
    }

    private List<ProcessUnderstandingStage> sanitizeStages(List<ProcessUnderstandingStage> stages) {
        if (stages == null) {
            return null;
        }
        return stages.stream()
                .map(stage -> stage == null ? null : new ProcessUnderstandingStage(
                        sanitizeText(stage.id()),
                        sanitizeText(stage.title()),
                        sanitizeText(stage.description()),
                        stage.provenance(),
                        stage.operationType(),
                        stage.inputNature()))
                .toList();
    }

    private String sanitizeText(String value) {
        return value == null ? null : value.strip();
    }

    private String textValue(JsonNode node) {
        return node != null && node.isTextual() ? node.textValue() : null;
    }

    private long durationMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
