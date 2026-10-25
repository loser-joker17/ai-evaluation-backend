package com.aievaluation.backend.dto.response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProgressResponse {

    private int evaluatedAnswers;                      // how many completed answers this covers
    private BigDecimal averageScorePercent;            // total score / max marks, averaged
    private String weakestDimension;                   // lowest average; null if no data
    private List<DimensionProgressResponse> dimensions = new ArrayList<>();

    public int getEvaluatedAnswers() { return evaluatedAnswers; }
    public void setEvaluatedAnswers(int evaluatedAnswers) { this.evaluatedAnswers = evaluatedAnswers; }

    public BigDecimal getAverageScorePercent() { return averageScorePercent; }
    public void setAverageScorePercent(BigDecimal averageScorePercent) { this.averageScorePercent = averageScorePercent; }

    public String getWeakestDimension() { return weakestDimension; }
    public void setWeakestDimension(String weakestDimension) { this.weakestDimension = weakestDimension; }

    public List<DimensionProgressResponse> getDimensions() { return dimensions; }
    public void setDimensions(List<DimensionProgressResponse> dimensions) { this.dimensions = dimensions; }
}