package com.aievaluation.backend.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Matches the evaluator's EvaluationInput (app/schemas.py). */
public class EvaluatorInput {

    private String question;
    private String answer;

    @JsonProperty("max_marks")
    private int maxMarks;

    @JsonProperty("word_limit")
    private int wordLimit;

    private String subject;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public int getMaxMarks() { return maxMarks; }
    public void setMaxMarks(int maxMarks) { this.maxMarks = maxMarks; }

    public int getWordLimit() { return wordLimit; }
    public void setWordLimit(int wordLimit) { this.wordLimit = wordLimit; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
}