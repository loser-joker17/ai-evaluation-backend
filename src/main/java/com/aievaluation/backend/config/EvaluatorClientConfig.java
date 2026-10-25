package com.aievaluation.backend.config;

import java.net.http.HttpClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class EvaluatorClientConfig {

    @Bean
    public RestClient evaluatorRestClient(EvaluatorProperties properties) {
        if (properties.getInternalKey() == null || properties.getInternalKey().isBlank()) {
            throw new IllegalStateException("EVALUATOR_INTERNAL_KEY is missing. Set it in .env.");
        }
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.getConnectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("X-Internal-Key", properties.getInternalKey())
                .requestFactory(requestFactory)
                .build();
    }
}