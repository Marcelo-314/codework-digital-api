package com.codeworkdigital.api.verification.infrastructure.turnstile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeworkdigital.api.verification.application.HumanVerificationContext;
import java.net.URI;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TurnstilePropertiesTest {

    @Test
    void acceptsValidConfigurationAndNormalizesHostnames() {
        TurnstileProperties properties = properties(Set.of("LOCALHOST", "example.test"));

        assertThat(properties.allowedHostnames()).containsExactlyInAnyOrder("localhost", "example.test");
        assertThat(properties.expectedAction(HumanVerificationContext.CONTACT_HOME)).isEqualTo("contact_home");
        assertThat(properties.expectedAction(HumanVerificationContext.CONTACT_PAGE)).isEqualTo("contact_page");
    }

    @Test
    void rejectsBlankSecret() {
        assertThatThrownBy(() -> new TurnstileProperties(
                " ",
                URI.create("https://example.test/siteverify"),
                Set.of("localhost"),
                "contact_home",
                "contact_page",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEmptyHostnames() {
        assertThatThrownBy(() -> properties(Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsHostnameWithScheme() {
        assertThatThrownBy(() -> properties(Set.of("https://localhost")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsHostnameWithPort() {
        assertThatThrownBy(() -> properties(Set.of("localhost:8080")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsWildcardHostname() {
        assertThatThrownBy(() -> properties(Set.of("*.example.test")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEmptyActions() {
        assertThatThrownBy(() -> new TurnstileProperties(
                "test-secret",
                URI.create("https://example.test/siteverify"),
                Set.of("localhost"),
                "",
                "contact_page",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsActionsLongerThanThirtyTwoCharacters() {
        assertThatThrownBy(() -> new TurnstileProperties(
                "test-secret",
                URI.create("https://example.test/siteverify"),
                Set.of("localhost"),
                "a".repeat(33),
                "contact_page",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsActionCharactersOutsideAllowedSet() {
        assertThatThrownBy(() -> new TurnstileProperties(
                "test-secret",
                URI.create("https://example.test/siteverify"),
                Set.of("localhost"),
                "contact.home",
                "contact_page",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsZeroOrNegativeTimeouts() {
        assertThatThrownBy(() -> new TurnstileProperties(
                "test-secret",
                URI.create("https://example.test/siteverify"),
                Set.of("localhost"),
                "contact_home",
                "contact_page",
                Duration.ZERO,
                Duration.ofSeconds(2)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new TurnstileProperties(
                "test-secret",
                URI.create("https://example.test/siteverify"),
                Set.of("localhost"),
                "contact_home",
                "contact_page",
                Duration.ofSeconds(1),
                Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsRelativeUrl() {
        assertThatThrownBy(() -> new TurnstileProperties(
                "test-secret",
                URI.create("/siteverify"),
                Set.of("localhost"),
                "contact_home",
                "contact_page",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toStringDoesNotExposeSecret() {
        TurnstileProperties properties = properties(Set.of("localhost"));

        assertThat(properties.toString()).doesNotContain("test-secret");
        assertThat(properties.toString()).contains("<redacted>");
    }

    private TurnstileProperties properties(Set<String> hostnames) {
        return new TurnstileProperties(
                "test-secret",
                URI.create("https://example.test/siteverify"),
                hostnames,
                "contact_home",
                "contact_page",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2));
    }
}
