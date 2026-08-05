package com.codeworkdigital.api.shared.web;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ApiWebProperties.class)
public class ApiCorsConfiguration implements WebMvcConfigurer {

    private static final String CONTACT_SUBMISSIONS_PATH = "/api/v1/contact-submissions";

    private final ApiWebProperties properties;

    public ApiCorsConfiguration(ApiWebProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(CONTACT_SUBMISSIONS_PATH)
                .allowedOrigins(properties.allowedOrigins().toArray(String[]::new))
                .allowedMethods(HttpMethod.POST.name())
                .allowedHeaders(HttpHeaders.CONTENT_TYPE, "Idempotency-Key", HttpHeaders.ACCEPT)
                .allowCredentials(false)
                .maxAge(properties.corsMaxAge().toSeconds());
    }
}
