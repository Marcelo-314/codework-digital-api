package com.codeworkdigital.api.verification.infrastructure.turnstile;

import com.codeworkdigital.api.verification.application.HumanVerificationContext;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cwd.turnstile")
public final class TurnstileProperties {

    private static final Duration MAX_TIMEOUT = Duration.ofSeconds(30);

    private final String secretKey;
    private final URI siteverifyUrl;
    private final Set<String> allowedHostnames;
    private final String homeAction;
    private final String contactPageAction;
    private final Duration connectTimeout;
    private final Duration requestTimeout;

    public TurnstileProperties(
            String secretKey,
            URI siteverifyUrl,
            Set<String> allowedHostnames,
            String homeAction,
            String contactPageAction,
            Duration connectTimeout,
            Duration requestTimeout) {
        this.secretKey = requireText(secretKey, "secretKey");
        this.siteverifyUrl = requireSiteverifyUrl(siteverifyUrl);
        this.allowedHostnames = normalizeHostnames(allowedHostnames);
        this.homeAction = requireAction(homeAction, "homeAction");
        this.contactPageAction = requireAction(contactPageAction, "contactPageAction");
        this.connectTimeout = requireTimeout(connectTimeout, "connectTimeout");
        this.requestTimeout = requireTimeout(requestTimeout, "requestTimeout");
        if (this.requestTimeout.compareTo(this.connectTimeout) < 0) {
            throw new IllegalArgumentException("requestTimeout must not be lower than connectTimeout");
        }
    }

    public String secretKey() {
        return secretKey;
    }

    public URI siteverifyUrl() {
        return siteverifyUrl;
    }

    public Set<String> allowedHostnames() {
        return allowedHostnames;
    }

    public Duration connectTimeout() {
        return connectTimeout;
    }

    public Duration requestTimeout() {
        return requestTimeout;
    }

    public String expectedAction(HumanVerificationContext context) {
        return switch (context) {
            case CONTACT_HOME -> homeAction;
            case CONTACT_PAGE -> contactPageAction;
        };
    }

    public boolean isAllowedHostname(String hostname) {
        return hostname != null && allowedHostnames.contains(hostname.toLowerCase(Locale.ROOT));
    }

    @Override
    public String toString() {
        return "TurnstileProperties[secretKey=<redacted>, siteverifyUrl=" + siteverifyUrl
                + ", allowedHostnames=" + allowedHostnames
                + ", homeAction=" + homeAction
                + ", contactPageAction=" + contactPageAction
                + ", connectTimeout=" + connectTimeout
                + ", requestTimeout=" + requestTimeout
                + "]";
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private static URI requireSiteverifyUrl(URI uri) {
        if (uri == null || !uri.isAbsolute() || uri.getFragment() != null) {
            throw new IllegalArgumentException("siteverifyUrl must be an absolute URI without fragment");
        }
        String scheme = uri.getScheme();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("siteverifyUrl must use http or https");
        }
        return uri;
    }

    private static Set<String> normalizeHostnames(Set<String> hostnames) {
        if (hostnames == null || hostnames.isEmpty()) {
            throw new IllegalArgumentException("allowedHostnames is required");
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String hostname : hostnames) {
            if (hostname == null || hostname.isBlank()) {
                throw new IllegalArgumentException("allowedHostnames contains a blank hostname");
            }
            if (!hostname.equals(hostname.strip())
                    || hostname.contains("://")
                    || hostname.contains("/")
                    || hostname.contains(":")
                    || hostname.contains("*")) {
                throw new IllegalArgumentException("allowedHostnames contains an invalid hostname");
            }
            normalized.add(hostname.toLowerCase(Locale.ROOT));
        }
        return Set.copyOf(normalized);
    }

    private static String requireAction(String action, String name) {
        if (action == null || !action.matches("[A-Za-z0-9_-]{1,32}")) {
            throw new IllegalArgumentException(name + " is invalid");
        }
        return action;
    }

    private static Duration requireTimeout(Duration timeout, String name) {
        if (timeout == null || timeout.isZero() || timeout.isNegative() || timeout.compareTo(MAX_TIMEOUT) > 0) {
            throw new IllegalArgumentException(name + " is invalid");
        }
        return timeout;
    }
}
