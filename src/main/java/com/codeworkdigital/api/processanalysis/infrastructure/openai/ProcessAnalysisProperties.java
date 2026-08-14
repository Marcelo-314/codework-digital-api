package com.codeworkdigital.api.processanalysis.infrastructure.openai;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cwd.process-analysis")
public final class ProcessAnalysisProperties {

    private static final Duration MAX_TIMEOUT = Duration.ofSeconds(30);

    private final boolean demoEnabled;
    private final String apiKey;
    private final String model;
    private final URI responsesUrl;
    private final Duration connectTimeout;
    private final Duration requestTimeout;

    public ProcessAnalysisProperties(
            boolean demoEnabled,
            String apiKey,
            String model,
            URI responsesUrl,
            Duration connectTimeout,
            Duration requestTimeout) {
        this.demoEnabled = demoEnabled;
        this.apiKey = demoEnabled ? requireText(apiKey, "apiKey") : blankToEmpty(apiKey);
        this.model = demoEnabled ? requireText(model, "model") : blankToEmpty(model);
        this.responsesUrl = requireResponsesUrl(responsesUrl);
        this.connectTimeout = requireTimeout(connectTimeout, "connectTimeout");
        this.requestTimeout = requireTimeout(requestTimeout, "requestTimeout");
        if (this.requestTimeout.compareTo(this.connectTimeout) < 0) {
            throw new IllegalArgumentException("requestTimeout must not be lower than connectTimeout");
        }
    }

    public boolean demoEnabled() {
        return demoEnabled;
    }

    public String apiKey() {
        return apiKey;
    }

    public String model() {
        return model;
    }

    public URI responsesUrl() {
        return responsesUrl;
    }

    public Duration connectTimeout() {
        return connectTimeout;
    }

    public Duration requestTimeout() {
        return requestTimeout;
    }

    @Override
    public String toString() {
        return "ProcessAnalysisProperties[demoEnabled=" + demoEnabled
                + ", apiKey=<redacted>, model=" + model
                + ", responsesUrl=" + responsesUrl
                + ", connectTimeout=" + connectTimeout
                + ", requestTimeout=" + requestTimeout + "]";
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.strip())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static URI requireResponsesUrl(URI uri) {
        if (uri == null || !uri.isAbsolute() || uri.getFragment() != null) {
            throw new IllegalArgumentException("responsesUrl must be an absolute URI without fragment");
        }
        String scheme = uri.getScheme();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("responsesUrl must use http or https");
        }
        return uri;
    }

    private static Duration requireTimeout(Duration timeout, String field) {
        if (timeout == null || timeout.isZero() || timeout.isNegative() || timeout.compareTo(MAX_TIMEOUT) > 0) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return timeout;
    }
}
