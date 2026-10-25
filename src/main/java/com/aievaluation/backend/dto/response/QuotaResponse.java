package com.aievaluation.backend.dto.response;

import java.time.Instant;

public class QuotaResponse {

    private int dailyLimit;
    private int usedToday;
    private int remainingToday;
    private Instant resetsAt;

    public int getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(int dailyLimit) { this.dailyLimit = dailyLimit; }

    public int getUsedToday() { return usedToday; }
    public void setUsedToday(int usedToday) { this.usedToday = usedToday; }

    public int getRemainingToday() { return remainingToday; }
    public void setRemainingToday(int remainingToday) { this.remainingToday = remainingToday; }

    public Instant getResetsAt() { return resetsAt; }
    public void setResetsAt(Instant resetsAt) { this.resetsAt = resetsAt; }
}