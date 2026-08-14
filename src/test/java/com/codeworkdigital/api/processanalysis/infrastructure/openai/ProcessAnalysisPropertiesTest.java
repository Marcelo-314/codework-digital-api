package com.codeworkdigital.api.processanalysis.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ProcessAnalysisPropertiesTest {

    @Test
    void acceptsDisabledConfigurationWithoutCredentials() {
        ProcessAnalysisProperties properties = new ProcessAnalysisProperties(
                false,
                "",
                "",
                URI.create("https://api.openai.com/v1/responses"),
                Duration.ofSeconds(2),
                Duration.ofSeconds(8));

        assertThat(properties.demoEnabled()).isFalse();
        assertThat(properties.apiKey()).isEmpty();
        assertThat(properties.model()).isEmpty();
    }

    @Test
    void enabledConfigurationRequiresApiKeyAndModel() {
        assertThatThrownBy(() -> new ProcessAnalysisProperties(
                true,
                " ",
                "gpt-test-structured",
                URI.create("https://api.openai.com/v1/responses"),
                Duration.ofSeconds(2),
                Duration.ofSeconds(8)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new ProcessAnalysisProperties(
                true,
                "test-api-key",
                " ",
                URI.create("https://api.openai.com/v1/responses"),
                Duration.ofSeconds(2),
                Duration.ofSeconds(8)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsRelativeResponsesUrl() {
        assertThatThrownBy(() -> new ProcessAnalysisProperties(
                false,
                "",
                "",
                URI.create("/v1/responses"),
                Duration.ofSeconds(2),
                Duration.ofSeconds(8)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsRequestTimeoutBelowConnectTimeout() {
        assertThatThrownBy(() -> new ProcessAnalysisProperties(
                false,
                "",
                "",
                URI.create("https://api.openai.com/v1/responses"),
                Duration.ofSeconds(3),
                Duration.ofSeconds(2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toStringDoesNotExposeApiKey() {
        ProcessAnalysisProperties properties = new ProcessAnalysisProperties(
                true,
                "test-api-key",
                "gpt-test-structured",
                URI.create("https://api.openai.com/v1/responses"),
                Duration.ofSeconds(2),
                Duration.ofSeconds(8));

        assertThat(properties.toString()).contains("<redacted>");
        assertThat(properties.toString()).doesNotContain("test-api-key");
    }
}
