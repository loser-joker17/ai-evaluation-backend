package com.aievaluation.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.quota")
public class QuotaProperties {

    private int dailySubmissions = 5;
    // The day resets at midnight in this zone (our users are in India; the server runs in UTC).
    private String zone = "Asia/Kolkata";

    public int getDailySubmissions() { return dailySubmissions; }
    public void setDailySubmissions(int dailySubmissions) { this.dailySubmissions = dailySubmissions; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
}