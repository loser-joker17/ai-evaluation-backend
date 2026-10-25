package com.aievaluation.backend.mapper;

import org.springframework.stereotype.Component;

import com.aievaluation.backend.dto.response.EvaluationSummaryResponse;
import com.aievaluation.backend.entity.Evaluation;

@Component
public class EvaluationMapper {

    public EvaluationSummaryResponse toSummary(Evaluation evaluation) {
        EvaluationSummaryResponse response = new EvaluationSummaryResponse();
        response.setId(evaluation.getId());
        response.setAttemptNo(evaluation.getAttemptNo());
        response.setStatus(evaluation.getStatus().name());
        response.setTotalScore(evaluation.getTotalScore());
        response.setMaxMarks(evaluation.getMaxMarks());
        response.setErrorCode(evaluation.getErrorCode() == null ? null : evaluation.getErrorCode().name());
        response.setCreatedAt(evaluation.getCreatedAt());
        response.setFinishedAt(evaluation.getFinishedAt());
        return response;
    }
}