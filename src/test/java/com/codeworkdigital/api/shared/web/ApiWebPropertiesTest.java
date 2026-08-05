package com.codeworkdigital.api.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ApiWebPropertiesTest {

    @Test
    void acceptsValidConfiguration() {
        ApiWebProperties properties = properties(Set.of("https://example.test"));

        assertThat(properties.allowedOrigins()).containsExactly("https://example.test");
        assertThat(properties.corsMaxAge()).isEqualTo(Duration.ofHours(1));
        assertThat(properties.maxContactRequestBytes()).isEqualTo(65536);
    }

    @Test
    void acceptsMultipleOrigins() {
        ApiWebProperties properties = properties(Set.of("https://example.test", "https://www.example.test"));

        assertThat(properties.allowedOrigins()).containsExactlyInAnyOrder(
                "https://example.test",
                "https://www.example.test");
    }

    @Test
    void normalizesOriginSchemeAndHostToLowercase() {
        ApiWebProperties properties = properties(Set.of("HTTPS://Example.TEST"));

        assertThat(properties.allowedOrigins()).containsExactly("https://example.test");
    }

    @Test
    void acceptsOriginWithPort() {
        ApiWebProperties properties = properties(Set.of("http://localhost:3000"));

        assertThat(properties.allowedOrigins()).containsExactly("http://localhost:3000");
    }

    @Test
    void rejectsOriginWithInvalidPort() {
        assertInvalidOrigins(Set.of("https://example.test:abc"));
    }

    @Test
    void rejectsEmptyOrigins() {
        assertInvalidOrigins(Set.of());
    }

    @Test
    void rejectsWildcardOrigin() {
        assertInvalidOrigins(Set.of("*"));
    }

    @Test
    void rejectsOriginPattern() {
        assertInvalidOrigins(Set.of("https://*.example.test"));
    }

    @Test
    void rejectsOriginWithoutScheme() {
        assertInvalidOrigins(Set.of("example.test"));
    }

    @Test
    void rejectsUnsupportedScheme() {
        assertInvalidOrigins(Set.of("ftp://example.test"));
    }

    @Test
    void rejectsPath() {
        assertInvalidOrigins(Set.of("https://example.test/path"));
    }

    @Test
    void rejectsTrailingSlash() {
        assertInvalidOrigins(Set.of("https://example.test/"));
    }

    @Test
    void rejectsQuery() {
        assertInvalidOrigins(Set.of("https://example.test?x=1"));
    }

    @Test
    void rejectsFragment() {
        assertInvalidOrigins(Set.of("https://example.test#fragment"));
    }

    @Test
    void rejectsUserInfo() {
        assertInvalidOrigins(Set.of("https://user@example.test"));
    }

    @Test
    void rejectsBlankOrigin() {
        assertInvalidOrigins(Set.of(" "));
    }

    @Test
    void rejectsZeroOrNegativeMaxAge() {
        assertThatThrownBy(() -> new ApiWebProperties(Set.of("https://example.test"), Duration.ZERO, 65536))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApiWebProperties(Set.of("https://example.test"), Duration.ofSeconds(-1), 65536))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMaxAgeAboveTwentyFourHours() {
        assertThatThrownBy(() -> new ApiWebProperties(Set.of("https://example.test"), Duration.ofHours(25), 65536))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsZeroOrNegativeBodyLimit() {
        assertThatThrownBy(() -> new ApiWebProperties(Set.of("https://example.test"), Duration.ofHours(1), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApiWebProperties(Set.of("https://example.test"), Duration.ofHours(1), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBodyLimitAboveOneMib() {
        assertThatThrownBy(() -> new ApiWebProperties(Set.of("https://example.test"), Duration.ofHours(1), 1024 * 1024 + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ApiWebProperties properties(Set<String> origins) {
        return new ApiWebProperties(origins, Duration.ofHours(1), 65536);
    }

    private void assertInvalidOrigins(Set<String> origins) {
        assertThatThrownBy(() -> properties(origins)).isInstanceOf(IllegalArgumentException.class);
    }
}
