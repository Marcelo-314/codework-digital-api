package com.codeworkdigital.api.shared.web;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cwd.web")
public final class ApiWebProperties {

    private static final Duration MAX_CORS_MAX_AGE = Duration.ofHours(24);
    private static final int MAX_CONTACT_REQUEST_BYTES = 1024 * 1024;

    private final Set<String> allowedOrigins;
    private final Duration corsMaxAge;
    private final int maxContactRequestBytes;

    public ApiWebProperties(Set<String> allowedOrigins, Duration corsMaxAge, int maxContactRequestBytes) {
        this.allowedOrigins = normalizeOrigins(allowedOrigins);
        this.corsMaxAge = requireCorsMaxAge(corsMaxAge);
        this.maxContactRequestBytes = requireBodyLimit(maxContactRequestBytes);
    }

    public Set<String> allowedOrigins() {
        return allowedOrigins;
    }

    public Duration corsMaxAge() {
        return corsMaxAge;
    }

    public int maxContactRequestBytes() {
        return maxContactRequestBytes;
    }

    private static Set<String> normalizeOrigins(Set<String> origins) {
        if (origins == null || origins.isEmpty()) {
            throw new IllegalArgumentException("allowedOrigins is required");
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String origin : origins) {
            normalized.add(normalizeOrigin(origin));
        }
        return Set.copyOf(normalized);
    }

    private static String normalizeOrigin(String origin) {
        if (origin == null || origin.isBlank() || !origin.equals(origin.strip())) {
            throw new IllegalArgumentException("allowedOrigins contains an invalid origin");
        }
        if ("*".equals(origin) || origin.contains("*")) {
            throw new IllegalArgumentException("allowedOrigins contains an invalid origin");
        }

        URI uri;
        try {
            uri = URI.create(origin);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("allowedOrigins contains an invalid origin", exception);
        }

        String scheme = uri.getScheme();
        if (!uri.isAbsolute()
                || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null
                || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())) {
            throw new IllegalArgumentException("allowedOrigins contains an invalid origin");
        }

        StringBuilder value = new StringBuilder()
                .append(scheme.toLowerCase(Locale.ROOT))
                .append("://")
                .append(uri.getHost().toLowerCase(Locale.ROOT));
        if (uri.getPort() != -1) {
            value.append(':').append(uri.getPort());
        }
        return value.toString();
    }

    private static Duration requireCorsMaxAge(Duration value) {
        if (value == null || value.isZero() || value.isNegative() || value.compareTo(MAX_CORS_MAX_AGE) > 0) {
            throw new IllegalArgumentException("corsMaxAge is invalid");
        }
        return value;
    }

    private static int requireBodyLimit(int value) {
        if (value <= 0 || value > MAX_CONTACT_REQUEST_BYTES) {
            throw new IllegalArgumentException("maxContactRequestBytes is invalid");
        }
        return value;
    }
}
