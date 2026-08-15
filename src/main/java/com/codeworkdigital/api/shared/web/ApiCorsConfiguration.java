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
    private static final String PROCESS_ANALYSIS_PATH = "/api/labs/process-analysis";

    private final ApiWebProperties properties;

    public ApiCorsConfiguration(ApiWebProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registerPostJsonCors(registry, CONTACT_SUBMISSIONS_PATH, "Idempotency-Key", HttpHeaders.ACCEPT);
        registerPostJsonCors(registry, PROCESS_ANALYSIS_PATH);
    }

    private void registerPostJsonCors(CorsRegistry registry, String path, String... extraAllowedHeaders) {
        registry.addMapping(path)
                .allowedOrigins(properties.allowedOrigins().toArray(String[]::new))
                .allowedMethods(HttpMethod.POST.name())
                .allowedHeaders(allowedHeaders(extraAllowedHeaders))
                .allowCredentials(false)
                .maxAge(properties.corsMaxAge().toSeconds());
    }

    private static String[] allowedHeaders(String... extraAllowedHeaders) {
        if (extraAllowedHeaders == null || extraAllowedHeaders.length == 0) {
            return new String[] {HttpHeaders.CONTENT_TYPE};
        }

        String[] headers = new String[extraAllowedHeaders.length + 1];
        headers[0] = HttpHeaders.CONTENT_TYPE;
        System.arraycopy(extraAllowedHeaders, 0, headers, 1, extraAllowedHeaders.length);
        return headers;
    }
}
