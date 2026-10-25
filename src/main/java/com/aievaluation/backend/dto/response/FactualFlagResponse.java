package com.aievaluation.backend.dto.response;

import java.util.ArrayList;
import java.util.List;

/** A claim in the answer that official sources contradict. */
public class FactualFlagResponse {

    private String claim;
    private String explanation;
    private List<String> sources = new ArrayList<>();

    public String getClaim() { return claim; }
    public void setClaim(String claim) { this.claim = claim; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public List<String> getSources() { return sources; }
    public void setSources(List<String> sources) { this.sources = sources; }
}