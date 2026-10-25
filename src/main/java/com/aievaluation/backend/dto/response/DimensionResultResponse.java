package com.aievaluation.backend.dto.response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DimensionResultResponse {

    private String dimension;      // relevance, content_substantiation, ...
    private String label;          // "Relevance", "Content & substantiation", ...
    private BigDecimal score;
    private BigDecimal maxScore;
    private String justification;
    private List<String> evidence = new ArrayList<>();
    private String improvement;

    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }

    public BigDecimal getMaxScore() { return maxScore; }
    public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }

    public List<String> getEvidence() { return evidence; }
    public void setEvidence(List<String> evidence) { this.evidence = evidence; }

    public String getImprovement() { return improvement; }
    public void setImprovement(String improvement) { this.improvement = improvement; }
}