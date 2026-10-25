package com.aievaluation.backend.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Body of POST /jobs on the evaluator. */
public class EvaluatorJobRequest {

    @JsonProperty("job_id")
    private String jobId;

    private EvaluatorInput input;

    public EvaluatorJobRequest() {
    }

    public EvaluatorJobRequest(String jobId, EvaluatorInput input) {
        this.jobId = jobId;
        this.input = input;
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public EvaluatorInput getInput() { return input; }
    public void setInput(EvaluatorInput input) { this.input = input; }
}