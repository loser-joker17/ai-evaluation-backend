package com.aievaluation.backend.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** What the student sees for a finished evaluation. No evaluator internals. */
public class EvaluationResultResponse {

    private UUID evaluationId;
    private UUID submissionId;
    private int attemptNo;
    private String status;                 // COMPLETED or REJECTED
    private BigDecimal totalScore;
    private int maxMarks;
    private Integer wordCount;
    private Integer wordLimit;
    private String wordLimitStatus;        // within_limit, over_limit, under_limit ...
    private List<DimensionResultResponse> dimensions = new ArrayList<>();
    private List<String> strengths = new ArrayList<>();
    private List<String> weaknesses = new ArrayList<>();
    private List<String> improvementSuggestions = new ArrayList<>();
    private String overallFeedback;
    private List<FactualFlagResponse> factualFlags = new ArrayList<>();
    private List<String> rejectionReasons = new ArrayList<>();   // only when REJECTED
    private Instant finishedAt;

    public UUID getEvaluationId() { return evaluationId; }
    public void setEvaluationId(UUID evaluationId) { this.evaluationId = evaluationId; }

    public UUID getSubmissionId() { return submissionId; }
    public void setSubmissionId(UUID submissionId) { this.submissionId = submissionId; }

    public int getAttemptNo() { return attemptNo; }
    public void setAttemptNo(int attemptNo) { this.attemptNo = attemptNo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }

    public int getMaxMarks() { return maxMarks; }
    public void setMaxMarks(int maxMarks) { this.maxMarks = maxMarks; }

    public Integer getWordCount() { return wordCount; }
    public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }

    public Integer getWordLimit() { return wordLimit; }
    public void setWordLimit(Integer wordLimit) { this.wordLimit = wordLimit; }

    public String getWordLimitStatus() { return wordLimitStatus; }
    public void setWordLimitStatus(String wordLimitStatus) { this.wordLimitStatus = wordLimitStatus; }

    public List<DimensionResultResponse> getDimensions() { return dimensions; }
    public void setDimensions(List<DimensionResultResponse> dimensions) { this.dimensions = dimensions; }

    public List<String> getStrengths() { return strengths; }
    public void setStrengths(List<String> strengths) { this.strengths = strengths; }

    public List<String> getWeaknesses() { return weaknesses; }
    public void setWeaknesses(List<String> weaknesses) { this.weaknesses = weaknesses; }

    public List<String> getImprovementSuggestions() { return improvementSuggestions; }
    public void setImprovementSuggestions(List<String> improvementSuggestions) { this.improvementSuggestions = improvementSuggestions; }

    public String getOverallFeedback() { return overallFeedback; }
    public void setOverallFeedback(String overallFeedback) { this.overallFeedback = overallFeedback; }

    public List<FactualFlagResponse> getFactualFlags() { return factualFlags; }
    public void setFactualFlags(List<FactualFlagResponse> factualFlags) { this.factualFlags = factualFlags; }

    public List<String> getRejectionReasons() { return rejectionReasons; }
    public void setRejectionReasons(List<String> rejectionReasons) { this.rejectionReasons = rejectionReasons; }

    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
}