package com.aievaluation.backend.dto.response;

import java.time.Instant;
import java.util.UUID;
import java.math.BigDecimal;

/** One row in the history list: no full answer text, just a preview of the question. */
public class SubmissionSummaryResponse {

    private UUID id;
    private String paper;
    private int maxMarks;
    private int wordLimit;
    private Integer wordCount;
    private String questionPreview;
    private String status;
    private Instant createdAt;
    private String evaluationStatus;   // PENDING ... COMPLETED / REJECTED / FAILED; null if none
    private BigDecimal totalScore;     // only when COMPLETED
    private Instant evaluatedAt;       // finished_at of the latest attempt

    public String getEvaluationStatus() { return evaluationStatus; }
    public void setEvaluationStatus(String evaluationStatus) { this.evaluationStatus = evaluationStatus; }

    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }

    public Instant getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getPaper() { return paper; }
    public void setPaper(String paper) { this.paper = paper; }

    public int getMaxMarks() { return maxMarks; }
    public void setMaxMarks(int maxMarks) { this.maxMarks = maxMarks; }

    public int getWordLimit() { return wordLimit; }
    public void setWordLimit(int wordLimit) { this.wordLimit = wordLimit; }

    public Integer getWordCount() { return wordCount; }
    public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }

    public String getQuestionPreview() { return questionPreview; }
    public void setQuestionPreview(String questionPreview) { this.questionPreview = questionPreview; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}