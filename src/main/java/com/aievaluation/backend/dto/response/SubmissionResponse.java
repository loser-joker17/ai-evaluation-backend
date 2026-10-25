package com.aievaluation.backend.dto.response;

import java.time.Instant;
import java.util.UUID;

public class SubmissionResponse {

    private UUID id;
    private String sourceType;
    private String status;
    private String paper;
    private int maxMarks;
    private int wordLimit;
    private Integer wordCount;
    private String questionText;
    private String answerText;
    private Instant createdAt;
    private Instant submittedAt;
    private EvaluationSummaryResponse evaluation;

    public EvaluationSummaryResponse getEvaluation() { return evaluation; }
    public void setEvaluation(EvaluationSummaryResponse evaluation) { this.evaluation = evaluation; }
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaper() { return paper; }
    public void setPaper(String paper) { this.paper = paper; }

    public int getMaxMarks() { return maxMarks; }
    public void setMaxMarks(int maxMarks) { this.maxMarks = maxMarks; }

    public int getWordLimit() { return wordLimit; }
    public void setWordLimit(int wordLimit) { this.wordLimit = wordLimit; }

    public Integer getWordCount() { return wordCount; }
    public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
}