package com.aievaluation.backend.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aievaluation.backend.dto.response.DimensionProgressResponse;
import com.aievaluation.backend.dto.response.DimensionResultResponse;
import com.aievaluation.backend.dto.response.ProgressResponse;
import com.aievaluation.backend.entity.Evaluation;
import com.aievaluation.backend.enums.EvaluationStatus;
import com.aievaluation.backend.mapper.EvaluationResultMapper;
import com.aievaluation.backend.repository.EvaluationRepository;
import com.aievaluation.backend.service.ProgressService;

@Service
public class ProgressServiceImpl implements ProgressService {

    private static final int MAX_LAST = 50;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final EvaluationRepository evaluationRepository;
    private final EvaluationResultMapper evaluationResultMapper;

    public ProgressServiceImpl(EvaluationRepository evaluationRepository,
                               EvaluationResultMapper evaluationResultMapper) {
        this.evaluationRepository = evaluationRepository;
        this.evaluationResultMapper = evaluationResultMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public ProgressResponse forUser(UUID userId, int last) {
        int limit = Math.min(Math.max(last, 1), MAX_LAST);
        List<Evaluation> recent = evaluationRepository.findRecentByUserAndStatus(
                userId, EvaluationStatus.COMPLETED, PageRequest.of(0, limit));

        ProgressResponse response = new ProgressResponse();
        response.setEvaluatedAnswers(recent.size());
        if (recent.isEmpty()) {
            return response;
        }

        // Fixed order, so the frontend always gets the dimensions the same way.
        Map<String, DimensionProgressResponse> byDimension = new LinkedHashMap<>();
        List<BigDecimal> totalPercents = new ArrayList<>();

        // recent is newest first; walk it backwards so the chart series is oldest first.
        for (int i = recent.size() - 1; i >= 0; i--) {
            Evaluation evaluation = recent.get(i);
            BigDecimal totalPercent = percent(evaluation.getTotalScore(), BigDecimal.valueOf(evaluation.getMaxMarks()));
            if (totalPercent != null) {
                totalPercents.add(totalPercent);
            }
            for (DimensionResultResponse dimension : evaluationResultMapper.dimensionsOf(evaluation)) {
                BigDecimal p = percent(dimension.getScore(), dimension.getMaxScore());
                if (dimension.getDimension() == null || p == null) {
                    continue;
                }
                DimensionProgressResponse progress = byDimension.get(dimension.getDimension());
                if (progress == null) {
                    progress = new DimensionProgressResponse();
                    progress.setDimension(dimension.getDimension());
                    progress.setLabel(dimension.getLabel());
                    byDimension.put(dimension.getDimension(), progress);
                }
                progress.getRecentPercents().add(p);
            }
        }

        response.setAverageScorePercent(average(totalPercents));
        DimensionProgressResponse weakest = null;
        for (DimensionProgressResponse progress : byDimension.values()) {
            progress.setAveragePercent(average(progress.getRecentPercents()));
            if (weakest == null || progress.getAveragePercent().compareTo(weakest.getAveragePercent()) < 0) {
                weakest = progress;
            }
            response.getDimensions().add(progress);
        }
        response.setWeakestDimension(weakest == null ? null : weakest.getDimension());
        return response;
    }

    private static BigDecimal percent(BigDecimal score, BigDecimal max) {
        if (score == null || max == null || max.signum() <= 0) {
            return null;
        }
        return score.multiply(HUNDRED).divide(max, 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            sum = sum.add(value);
        }
        return sum.divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
    }
}