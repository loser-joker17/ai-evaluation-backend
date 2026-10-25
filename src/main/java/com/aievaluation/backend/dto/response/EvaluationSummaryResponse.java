package com.aievaluation.backend.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** The latest attempt's status and score. The full feedback comes in step 7. */
public class EvaluationSummaryResponse {

    private UUID id;
    private int attemptNo;
    private String status;
    private BigDecimal totalScore;
    private int maxMarks;
    private String errorCode;
    private Instant createdAt;
    private Instant finishedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public int getAttemptNo() { return attemptNo; }
    public void setAttemptNo(int attemptNo) { this.attemptNo = attemptNo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }

    public int getMaxMarks() { return maxMarks; }
    public void setMaxMarks(int maxMarks) { this.maxMarks = maxMarks; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
}