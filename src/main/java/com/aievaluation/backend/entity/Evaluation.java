package com.aievaluation.backend.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.aievaluation.backend.enums.EvaluationErrorCode;
import com.aievaluation.backend.enums.EvaluationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "evaluations")
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvaluationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_code", length = 30)
    private EvaluationErrorCode errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "dispatch_count", nullable = false)
    private Integer dispatchCount = 0;

    @Column(name = "next_dispatch_at")
    private Instant nextDispatchAt;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "queued_at")
    private Instant queuedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "total_score", precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "max_marks", nullable = false)
    private Integer maxMarks;

    @Column(name = "evaluator_version", length = 40)
    private String evaluatorVersion;

    @Column(name = "feedback_consistent")
    private Boolean feedbackConsistent;

    // Stored as-is from the evaluator. Read in step 7 to build what the student sees.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_json", columnDefinition = "jsonb")
    private String resultJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "run_info", columnDefinition = "jsonb")
    private String runInfo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "lost_count", nullable = false)
    private Integer lostCount = 0;

    public Integer getLostCount() { return lostCount; }
    public void setLostCount(Integer lostCount) { this.lostCount = lostCount; }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }

    public Submission getSubmission() { return submission; }
    public void setSubmission(Submission submission) { this.submission = submission; }

    public Integer getAttemptNo() { return attemptNo; }
    public void setAttemptNo(Integer attemptNo) { this.attemptNo = attemptNo; }

    public EvaluationStatus getStatus() { return status; }
    public void setStatus(EvaluationStatus status) { this.status = status; }

    public EvaluationErrorCode getErrorCode() { return errorCode; }
    public void setErrorCode(EvaluationErrorCode errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getDispatchCount() { return dispatchCount; }
    public void setDispatchCount(Integer dispatchCount) { this.dispatchCount = dispatchCount; }

    public Instant getNextDispatchAt() { return nextDispatchAt; }
    public void setNextDispatchAt(Instant nextDispatchAt) { this.nextDispatchAt = nextDispatchAt; }

    public Instant getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(Instant lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }

    public Instant getQueuedAt() { return queuedAt; }
    public void setQueuedAt(Instant queuedAt) { this.queuedAt = queuedAt; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }

    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }

    public Integer getMaxMarks() { return maxMarks; }
    public void setMaxMarks(Integer maxMarks) { this.maxMarks = maxMarks; }

    public String getEvaluatorVersion() { return evaluatorVersion; }
    public void setEvaluatorVersion(String evaluatorVersion) { this.evaluatorVersion = evaluatorVersion; }

    public Boolean getFeedbackConsistent() { return feedbackConsistent; }
    public void setFeedbackConsistent(Boolean feedbackConsistent) { this.feedbackConsistent = feedbackConsistent; }

    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }

    public String getRunInfo() { return runInfo; }
    public void setRunInfo(String runInfo) { this.runInfo = runInfo; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
}