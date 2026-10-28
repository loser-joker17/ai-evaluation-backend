package com.aievaluation.backend.service;

import java.util.UUID;

import com.aievaluation.backend.dto.request.CreateSubmissionRequest;
import com.aievaluation.backend.dto.response.EvaluationResultResponse;
import com.aievaluation.backend.dto.response.PageResponse;
import com.aievaluation.backend.dto.response.SubmissionResponse;
import com.aievaluation.backend.dto.response.SubmissionSummaryResponse;

public interface SubmissionService {

    SubmissionResponse create(UUID userId, CreateSubmissionRequest request);

    SubmissionResponse getForUser(UUID userId, UUID submissionId);

    PageResponse<SubmissionSummaryResponse> listForUser(UUID userId, int page, int size);

    void delete(UUID userId, UUID submissionId);
    EvaluationResultResponse getResult(UUID userId, UUID submissionId);
    SubmissionResponse retry(UUID userId, UUID submissionId);
}