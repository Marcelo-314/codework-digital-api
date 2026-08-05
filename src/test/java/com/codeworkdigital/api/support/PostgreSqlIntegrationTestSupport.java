package com.codeworkdigital.api.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public abstract class PostgreSqlIntegrationTestSupport {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:17-alpine");

    @Container
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE);

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("cwd.web.allowed-origins", () -> "http://localhost:3000");
        registry.add("cwd.web.cors-max-age", () -> "1h");
        registry.add("cwd.web.max-contact-request-bytes", () -> "65536");
        registry.add("cwd.turnstile.secret-key", () -> "test-secret-not-real");
        registry.add("cwd.turnstile.siteverify-url", () -> "http://localhost:9/siteverify");
        registry.add("cwd.turnstile.allowed-hostnames", () -> "localhost");
        registry.add("cwd.turnstile.home-action", () -> "contact_home");
        registry.add("cwd.turnstile.contact-page-action", () -> "contact_page");
        registry.add("cwd.turnstile.connect-timeout", () -> "100ms");
        registry.add("cwd.turnstile.request-timeout", () -> "200ms");
    }
}
