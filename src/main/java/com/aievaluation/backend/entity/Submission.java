package com.aievaluation.backend.entity;

import java.time.Instant;
import java.util.UUID;

import com.aievaluation.backend.enums.Paper;
import com.aievaluation.backend.enums.SourceType;
import com.aievaluation.backend.enums.SubmissionStatus;

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
@Table(name = "submissions")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubmissionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Paper paper;

    @Column(name = "max_marks", nullable = false)
    private Integer maxMarks;

    @Column(name = "word_limit", nullable = false)
    private Integer wordLimit;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

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

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public SourceType getSourceType() { return sourceType; }
    public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }

    public SubmissionStatus getStatus() { return status; }
    public void setStatus(SubmissionStatus status) { this.status = status; }

    public Paper getPaper() { return paper; }
    public void setPaper(Paper paper) { this.paper = paper; }

    public Integer getMaxMarks() { return maxMarks; }
    public void setMaxMarks(Integer maxMarks) { this.maxMarks = maxMarks; }

    public Integer getWordLimit() { return wordLimit; }
    public void setWordLimit(Integer wordLimit) { this.wordLimit = wordLimit; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }

    public Integer getWordCount() { return wordCount; }
    public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
}