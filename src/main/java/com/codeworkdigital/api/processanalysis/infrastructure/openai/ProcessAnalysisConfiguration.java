package com.codeworkdigital.api.processanalysis.infrastructure.openai;

import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisModelClient;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisUnavailableException;
import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ProcessAnalysisProperties.class)
public class ProcessAnalysisConfiguration {

    @Bean
    HttpClient processAnalysisHttpClient(ProcessAnalysisProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Bean
    ProcessAnalysisModelClient processAnalysisModelClient(
            HttpClient processAnalysisHttpClient,
            ObjectMapper objectMapper,
            ProcessAnalysisProperties properties) {
        if (!properties.demoEnabled()) {
            return command -> {
                throw new ProcessAnalysisUnavailableException("feature_disabled");
            };
        }
        return new OpenAiProcessAnalysisModelClient(processAnalysisHttpClient, objectMapper, properties);
    }
}
