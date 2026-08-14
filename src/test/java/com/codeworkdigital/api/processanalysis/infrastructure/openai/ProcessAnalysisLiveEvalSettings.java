package com.codeworkdigital.api.processanalysis.infrastructure.openai;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.function.Function;

final class ProcessAnalysisLiveEvalSettings {

    static final String ENABLED_KEY = "PROCESS_ANALYSIS_LIVE_EVAL_ENABLED";
    static final String RUNS_KEY = "PROCESS_ANALYSIS_EVAL_RUNS";
    static final String API_KEY = "OPENAI_API_KEY";
    static final String MODEL_KEY = "PROCESS_ANALYSIS_MODEL";
    static final String RESPONSES_URL_KEY = "PROCESS_ANALYSIS_RESPONSES_URL";
    static final String CONNECT_TIMEOUT_KEY = "PROCESS_ANALYSIS_CONNECT_TIMEOUT";
    static final String REQUEST_TIMEOUT_KEY = "PROCESS_ANALYSIS_REQUEST_TIMEOUT";
    static final Path DEFAULT_OUTPUT_ROOT = Path.of("target", "process-analysis-evals");

    private final boolean enabled;
    private final int runs;
    private final String apiKey;
    private final String model;
    private final URI responsesUrl;
    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final Path outputRoot;

    private ProcessAnalysisLiveEvalSettings(
            boolean enabled,
            int runs,
            String apiKey,
            String model,
            URI responsesUrl,
            Duration connectTimeout,
            Duration requestTimeout,
            Path outputRoot) {
        this.enabled = enabled;
        this.runs = runs;
        this.apiKey = apiKey;
        this.model = model;
        this.responsesUrl = responsesUrl;
        this.connectTimeout = connectTimeout;
        this.requestTimeout = requestTimeout;
        this.outputRoot = outputRoot;
    }

    static ProcessAnalysisLiveEvalSettings load() {
        return fromLookup(ProcessAnalysisLiveEvalSettings::lookupConfig, DEFAULT_OUTPUT_ROOT);
    }

    static ProcessAnalysisLiveEvalSettings fromLookup(Function<String, String> lookup, Path outputRoot) {
        boolean enabled = parseBoolean(lookup.apply(ENABLED_KEY), false, ENABLED_KEY);
        if (!enabled) {
            return new ProcessAnalysisLiveEvalSettings(
                    false,
                    1,
                    "",
                    "",
                    URI.create("https://api.openai.com/v1/responses"),
                    Duration.ofSeconds(2),
                    Duration.ofSeconds(8),
                    outputRoot);
        }

        int runs = parseRuns(lookup.apply(RUNS_KEY));
        return new ProcessAnalysisLiveEvalSettings(
                true,
                runs,
                requireText(lookup.apply(API_KEY), API_KEY),
                requireText(lookup.apply(MODEL_KEY), MODEL_KEY),
                parseUri(lookup.apply(RESPONSES_URL_KEY), URI.create("https://api.openai.com/v1/responses")),
                parseDuration(lookup.apply(CONNECT_TIMEOUT_KEY), Duration.ofSeconds(2), CONNECT_TIMEOUT_KEY),
                parseDuration(lookup.apply(REQUEST_TIMEOUT_KEY), Duration.ofSeconds(8), REQUEST_TIMEOUT_KEY),
                outputRoot);
    }

    boolean enabled() {
        return enabled;
    }

    int runs() {
        return runs;
    }

    String model() {
        return model;
    }

    Path outputRoot() {
        return outputRoot;
    }

    ProcessAnalysisProperties toProperties() {
        return new ProcessAnalysisProperties(
                true,
                apiKey,
                model,
                responsesUrl,
                connectTimeout,
                requestTimeout);
    }

    private static String lookupConfig(String key) {
        String propertyValue = System.getProperty(key);
        return propertyValue != null ? propertyValue : System.getenv(key);
    }

    private static boolean parseBoolean(String value, boolean defaultValue, String field) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException(field + " must be true or false");
        };
    }

    private static int parseRuns(String value) {
        if (value == null || value.isBlank()) {
            return 1;
        }
        try {
            int parsed = Integer.parseInt(value.strip());
            if (parsed < 1 || parsed > 3) {
                throw new IllegalArgumentException(RUNS_KEY + " must be between 1 and 3");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(RUNS_KEY + " must be a number between 1 and 3", exception);
        }
    }

    private static URI parseUri(String value, URI defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return URI.create(value.strip());
    }

    private static Duration parseDuration(String value, Duration defaultValue, String field) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        String normalized = value.strip().toLowerCase(Locale.ROOT);
        try {
            if (normalized.endsWith("ms")) {
                return Duration.ofMillis(Long.parseLong(normalized.substring(0, normalized.length() - 2)));
            }
            if (normalized.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(normalized.substring(0, normalized.length() - 1)));
            }
            if (normalized.endsWith("m")) {
                return Duration.ofMinutes(Long.parseLong(normalized.substring(0, normalized.length() - 1)));
            }
            return Duration.parse(value.strip());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(field + " is invalid", exception);
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required when live eval is enabled");
        }
        return value.strip();
    }
}
