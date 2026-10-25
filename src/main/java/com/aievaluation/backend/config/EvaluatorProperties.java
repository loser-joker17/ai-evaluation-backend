package com.aievaluation.backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.evaluator")
public class EvaluatorProperties {

    private String baseUrl = "http://localhost:8000";
    private String internalKey;
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(30);
    private Duration dispatchInterval = Duration.ofSeconds(5);
    private int dispatchBatchSize = 5;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getInternalKey() { return internalKey; }
    public void setInternalKey(String internalKey) { this.internalKey = internalKey; }

    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }

    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }

    public Duration getDispatchInterval() { return dispatchInterval; }
    public void setDispatchInterval(Duration dispatchInterval) { this.dispatchInterval = dispatchInterval; }

    public int getDispatchBatchSize() { return dispatchBatchSize; }
    public void setDispatchBatchSize(int dispatchBatchSize) { this.dispatchBatchSize = dispatchBatchSize; }
}