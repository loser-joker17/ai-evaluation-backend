package com.aievaluation.backend.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.JsonNode;

/**
 * The evaluator's job, as sent to our callback and returned by GET /jobs/{id}.
 * "result" stays raw JSON: we store it as-is and only read a few fields from it.
 * Unknown fields are ignored, so a new evaluator field never causes a 400
 * (the evaluator stops retrying a callback on 4xx).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EvaluatorJobResponse {

    public static final String STATUS_QUEUED = "queued";
    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_DONE = "done";
    public static final String STATUS_FAILED = "failed";

    @JsonProperty("job_id")
    private String jobId;

    private String status;

    private JsonNode result;

    // Can contain answer text (it's the exception message). Store it, never log it.
    private String error;

    // Epoch seconds as floats.
    @JsonProperty("created_at")
    private Double createdAt;

    @JsonProperty("started_at")
    private Double startedAt;

    @JsonProperty("finished_at")
    private Double finishedAt;

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public JsonNode getResult() { return result; }
    public void setResult(JsonNode result) { this.result = result; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public Double getCreatedAt() { return createdAt; }
    public void setCreatedAt(Double createdAt) { this.createdAt = createdAt; }

    public Double getStartedAt() { return startedAt; }
    public void setStartedAt(Double startedAt) { this.startedAt = startedAt; }

    public Double getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Double finishedAt) { this.finishedAt = finishedAt; }
}