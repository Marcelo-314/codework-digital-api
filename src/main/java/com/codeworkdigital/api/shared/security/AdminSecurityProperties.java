package com.codeworkdigital.api.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cwd.admin")
public final class AdminSecurityProperties {

    private final String username;
    private final String password;

    public AdminSecurityProperties(String username, String password) {
        this.username = requireText(username, "username");
        this.password = requireText(password, "password");
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.strip())) {
            throw new IllegalArgumentException("cwd.admin." + field + " is required");
        }
        return value;
    }
}
