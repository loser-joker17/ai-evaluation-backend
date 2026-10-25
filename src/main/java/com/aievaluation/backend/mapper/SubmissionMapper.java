package com.aievaluation.backend.mapper;

import org.springframework.stereotype.Component;

import com.aievaluation.backend.dto.response.SubmissionResponse;
import com.aievaluation.backend.dto.response.SubmissionSummaryResponse;
import com.aievaluation.backend.entity.Submission;

@Component
public class SubmissionMapper {

    private static final int PREVIEW_LENGTH = 120;

    public SubmissionResponse toResponse(Submission submission) {
        SubmissionResponse response = new SubmissionResponse();
        response.setId(submission.getId());
        response.setSourceType(submission.getSourceType().name());
        response.setStatus(submission.getStatus().name());
        response.setPaper(submission.getPaper().name());
        response.setMaxMarks(submission.getMaxMarks());
        response.setWordLimit(submission.getWordLimit());
        response.setWordCount(submission.getWordCount());
        response.setQuestionText(submission.getQuestionText());
        response.setAnswerText(submission.getAnswerText());
        response.setCreatedAt(submission.getCreatedAt());
        response.setSubmittedAt(submission.getSubmittedAt());
        return response;
    }

    public SubmissionSummaryResponse toSummary(Submission submission) {
        SubmissionSummaryResponse response = new SubmissionSummaryResponse();
        response.setId(submission.getId());
        response.setPaper(submission.getPaper().name());
        response.setMaxMarks(submission.getMaxMarks());
        response.setWordLimit(submission.getWordLimit());
        response.setWordCount(submission.getWordCount());
        response.setQuestionPreview(preview(submission.getQuestionText()));
        response.setStatus(submission.getStatus().name());
        response.setCreatedAt(submission.getCreatedAt());
        return response;
    }

    private String preview(String text) {
        if (text.length() <= PREVIEW_LENGTH) {
            return text;
        }
        return text.substring(0, PREVIEW_LENGTH).strip() + "…";
    }
}