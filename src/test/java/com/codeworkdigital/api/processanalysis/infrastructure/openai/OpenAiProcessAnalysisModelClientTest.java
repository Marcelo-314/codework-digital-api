package com.codeworkdigital.api.processanalysis.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeworkdigital.api.processanalysis.application.AnalyzeProcessDescriptionCommand;
import com.codeworkdigital.api.processanalysis.application.InvalidProcessAnalysisModelResponseException;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisLocale;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisUnavailableException;
import com.codeworkdigital.api.processanalysis.application.ProcessUnderstanding;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

class OpenAiProcessAnalysisModelClientTest {

    private static final String API_KEY = "test-openai-api-key";
    private static final String MODEL = "gpt-test-structured";
    private static final String DESCRIPTION = "Orders arrive by WhatsApp, a person checks stock, and then confirms delivery.";
    private static final String PROVIDER_BODY = "{\"provider\":\"body\"}";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsExpectedResponsesApiRequestAndParsesStructuredOutput() throws Exception {
        AtomicReference<CapturedRequest> captured = new AtomicReference<>();
        AtomicInteger requests = new AtomicInteger();
        ProcessUnderstanding understanding = clientWithServer(exchange -> {
            requests.incrementAndGet();
            captured.set(capture(exchange));
            respond(exchange, 200, successResponse(validStructuredOutput()));
        }).analyze(validCommand());

        CapturedRequest request = captured.get();
        JsonNode body = objectMapper.readTree(request.body());

        assertThat(requests.get()).isEqualTo(1);
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.path()).isEqualTo("/v1/responses");
        assertThat(request.headers().get("Authorization")).contains("Bearer " + API_KEY);
        assertThat(body.get("model").textValue()).isEqualTo(MODEL);
        assertThat(body.get("store").booleanValue()).isFalse();
        assertThat(body.at("/text/format/type").textValue()).isEqualTo("json_schema");
        assertThat(body.at("/text/format/name").textValue()).isEqualTo("process_understanding_v1");
        assertThat(body.at("/text/format/strict").booleanValue()).isTrue();
        assertThat(body.at("/text/format/schema/additionalProperties").booleanValue()).isFalse();
        assertThat(body.at("/input/0/role").textValue()).isEqualTo("system");
        assertThat(body.at("/input/0/content").textValue())
                .contains("Do not recommend technology", "OBSERVED, INFERRED", "kebab-case");
        assertThat(body.at("/input/1/role").textValue()).isEqualTo("user");
        assertThat(body.at("/input/1/content").textValue()).isEqualTo(DESCRIPTION);
        assertThat(body.has("previous_response_id")).isFalse();
        assertThat(body.has("tools")).isFalse();

        assertThat(understanding.processDescription()).isEqualTo(DESCRIPTION);
        assertThat(understanding.observations()).hasSize(2);
        assertThat(understanding.stages()).hasSize(2);
    }

    @Test
    void treatsProviderTimeoutAsUnavailable() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        OpenAiProcessAnalysisModelClient client = clientWithServer(exchange -> {
            try {
                release.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, Duration.ofMillis(100), Duration.ofMillis(100));

        assertThatThrownBy(() -> client.analyze(validCommand()))
                .isInstanceOf(ProcessAnalysisUnavailableException.class);
        release.countDown();
    }

    @Test
    void treatsProviderHttp400AsUnavailable() throws Exception {
        assertThatThrownBy(() -> clientResponding(400, PROVIDER_BODY).analyze(validCommand()))
                .isInstanceOf(ProcessAnalysisUnavailableException.class);
    }

    @Test
    void treatsProviderHttp500AsUnavailable() throws Exception {
        assertThatThrownBy(() -> clientResponding(500, PROVIDER_BODY).analyze(validCommand()))
                .isInstanceOf(ProcessAnalysisUnavailableException.class);
    }

    @Test
    void rejectsNonCompletedResponseStatus() throws Exception {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", "incomplete");
        response.putArray("output");

        assertThatThrownBy(() -> clientResponding(200, objectMapper.writeValueAsString(response)).analyze(validCommand()))
                .isInstanceOf(InvalidProcessAnalysisModelResponseException.class);
    }

    @Test
    void rejectsResponseWithoutOutputText() throws Exception {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", "completed");
        response.putArray("output");

        assertThatThrownBy(() -> clientResponding(200, objectMapper.writeValueAsString(response)).analyze(validCommand()))
                .isInstanceOf(InvalidProcessAnalysisModelResponseException.class);
    }

    @Test
    void rejectsModelRefusal() throws Exception {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", "completed");
        ArrayNode output = response.putArray("output");
        ObjectNode message = output.addObject();
        message.put("type", "message");
        ArrayNode content = message.putArray("content");
        ObjectNode refusal = content.addObject();
        refusal.put("type", "refusal");
        refusal.put("refusal", "I cannot comply.");

        assertThatThrownBy(() -> clientResponding(200, objectMapper.writeValueAsString(response)).analyze(validCommand()))
                .isInstanceOf(InvalidProcessAnalysisModelResponseException.class);
    }

    @Test
    void rejectsStructuredOutputThatCannotBeConverted() throws Exception {
        assertThatThrownBy(() -> clientResponding(200, successResponse("{not-json")).analyze(validCommand()))
                .isInstanceOf(InvalidProcessAnalysisModelResponseException.class);
    }

    @Test
    void rejectsDuplicateStageIds() throws Exception {
        ObjectNode output = validStructuredOutput();
        ArrayNode stages = (ArrayNode) output.get("stages");
        ((ObjectNode) stages.get(1)).put("id", "receive-order");

        assertThatThrownBy(() -> clientResponding(200, successResponse(output)).analyze(validCommand()))
                .isInstanceOf(InvalidProcessAnalysisModelResponseException.class);
    }

    @Test
    void rejectsMoreStagesThanAllowed() throws Exception {
        ObjectNode output = validStructuredOutput();
        ArrayNode stages = objectMapper.createArrayNode();
        for (int index = 0; index < 13; index++) {
            stages.add(stage("stage-" + index));
        }
        output.set("stages", stages);

        assertThatThrownBy(() -> clientResponding(200, successResponse(output)).analyze(validCommand()))
                .isInstanceOf(InvalidProcessAnalysisModelResponseException.class);
    }

    @Test
    void rejectsUnexpectedStructuredOutputField() throws Exception {
        ObjectNode output = validStructuredOutput();
        output.put("unexpectedField", "value");

        assertThatThrownBy(() -> clientResponding(200, successResponse(output)).analyze(validCommand()))
                .isInstanceOf(InvalidProcessAnalysisModelResponseException.class);
    }

    @Test
    void exceptionsDoNotExposeDescriptionApiKeyOrProviderBody() throws Exception {
        assertThatThrownBy(() -> clientResponding(500, PROVIDER_BODY).analyze(validCommand()))
                .isInstanceOf(ProcessAnalysisUnavailableException.class)
                .hasMessageNotContaining(DESCRIPTION)
                .hasMessageNotContaining(API_KEY)
                .hasMessageNotContaining(PROVIDER_BODY);

        ObjectNode output = validStructuredOutput();
        ((ObjectNode) output.get("stages").get(0)).put("id", "Receive Order");

        assertThatThrownBy(() -> clientResponding(200, successResponse(output)).analyze(validCommand()))
                .isInstanceOf(InvalidProcessAnalysisModelResponseException.class)
                .hasMessageNotContaining(DESCRIPTION)
                .hasMessageNotContaining(API_KEY)
                .hasMessageNotContaining("Receive Order");
    }

    private OpenAiProcessAnalysisModelClient clientResponding(int status, String body) throws IOException {
        return clientWithServer(exchange -> respond(exchange, status, body));
    }

    private OpenAiProcessAnalysisModelClient clientWithServer(ExchangeHandler handler) throws IOException {
        return clientWithServer(handler, Duration.ofSeconds(1), Duration.ofSeconds(2));
    }

    private OpenAiProcessAnalysisModelClient clientWithServer(
            ExchangeHandler handler,
            Duration connectTimeout,
            Duration requestTimeout) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/responses", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();

        ProcessAnalysisProperties properties = new ProcessAnalysisProperties(
                true,
                API_KEY,
                MODEL,
                URI.create("http://localhost:" + server.getAddress().getPort() + "/v1/responses"),
                connectTimeout,
                requestTimeout);
        return new OpenAiProcessAnalysisModelClient(
                HttpClient.newBuilder()
                        .connectTimeout(connectTimeout)
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build(),
                objectMapper,
                properties);
    }

    private CapturedRequest capture(HttpExchange exchange) throws IOException {
        return new CapturedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                exchange.getRequestHeaders(),
                new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private AnalyzeProcessDescriptionCommand validCommand() {
        return new AnalyzeProcessDescriptionCommand(DESCRIPTION, ProcessAnalysisLocale.EN);
    }

    private String successResponse(String structuredOutput) throws IOException {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", "completed");
        ArrayNode output = response.putArray("output");
        ObjectNode message = output.addObject();
        message.put("type", "message");
        ArrayNode content = message.putArray("content");
        ObjectNode outputText = content.addObject();
        outputText.put("type", "output_text");
        outputText.put("text", structuredOutput);
        return objectMapper.writeValueAsString(response);
    }

    private String successResponse(ObjectNode structuredOutput) throws IOException {
        return successResponse(objectMapper.writeValueAsString(structuredOutput));
    }

    private ObjectNode validStructuredOutput() {
        ObjectNode output = objectMapper.createObjectNode();
        output.putArray("observations")
                .add("Orders arrive through WhatsApp.")
                .add("Someone checks stock before replying.");
        output.putArray("inferences")
                .add("The input is likely free-form and can require manual interpretation.");
        output.putArray("validationQuestions")
                .add("Where is the canonical stock data stored?");
        output.set("stages", objectMapper.createArrayNode()
                .add(stage("receive-order"))
                .add(stage("validate-stock")));
        output.put("preliminaryAssessment",
                "This understanding is preliminary and still depends on validating the stock source and exception handling.");
        return output;
    }

    private ObjectNode stage(String id) {
        ObjectNode stage = objectMapper.createObjectNode();
        stage.put("id", id);
        stage.put("title", "Stage " + id);
        stage.put("description", "Description for " + id + ".");
        stage.put("provenance", "OBSERVED");
        stage.put("operationType", "RECEIVE");
        stage.put("inputNature", "UNSTRUCTURED");
        return stage;
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private record CapturedRequest(
            String method,
            String path,
            com.sun.net.httpserver.Headers headers,
            String body) {
    }
}
