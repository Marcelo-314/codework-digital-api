package com.codeworkdigital.api.verification.infrastructure.turnstile;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TurnstileProperties.class)
public class TurnstileConfiguration {

    @Bean
    HttpClient turnstileHttpClient(TurnstileProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }
}
