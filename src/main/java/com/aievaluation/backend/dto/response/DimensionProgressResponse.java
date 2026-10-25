package com.aievaluation.backend.dto.response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DimensionProgressResponse {

    private String dimension;
    private String label;
    private BigDecimal averagePercent;                       // average of score / maxScore * 100
    private List<BigDecimal> recentPercents = new ArrayList<>();   // oldest first, for a chart

    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public BigDecimal getAveragePercent() { return averagePercent; }
    public void setAveragePercent(BigDecimal averagePercent) { this.averagePercent = averagePercent; }

    public List<BigDecimal> getRecentPercents() { return recentPercents; }
    public void setRecentPercents(List<BigDecimal> recentPercents) { this.recentPercents = recentPercents; }
}