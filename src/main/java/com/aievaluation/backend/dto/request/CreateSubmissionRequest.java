package com.aievaluation.backend.dto.request;

import com.aievaluation.backend.enums.Paper;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateSubmissionRequest {

    @NotNull(message = "Paper is required (GS1, GS2, GS3 or GS4)")
    private Paper paper;

    @NotNull(message = "Marks are required (10, 15 or 20)")
    private Integer maxMarks;

    @NotBlank(message = "Question is required")
    @Size(min = 5, max = 5000, message = "Question must be 5 to 5000 characters")
    private String questionText;

    @NotBlank(message = "Answer is required")
    @Size(max = 15000, message = "Answer must be at most 15000 characters")
    private String answerText;

    public Paper getPaper() { return paper; }
    public void setPaper(Paper paper) { this.paper = paper; }

    public Integer getMaxMarks() { return maxMarks; }
    public void setMaxMarks(Integer maxMarks) { this.maxMarks = maxMarks; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
}