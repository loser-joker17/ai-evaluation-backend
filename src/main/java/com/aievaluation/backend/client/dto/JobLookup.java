package com.aievaluation.backend.client.dto;

import com.aievaluation.backend.client.enums.JobLookupStatus;

/** Result of GET /jobs/{id}. job is set only when status is FOUND. */
public class JobLookup {

    private final JobLookupStatus status;
    private final EvaluatorJobResponse job;

    private JobLookup(JobLookupStatus status, EvaluatorJobResponse job) {
        this.status = status;
        this.job = job;
    }

    public static JobLookup found(EvaluatorJobResponse job) {
        return new JobLookup(JobLookupStatus.FOUND, job);
    }

    public static JobLookup notFound() {
        return new JobLookup(JobLookupStatus.NOT_FOUND, null);
    }

    public static JobLookup unavailable() {
        return new JobLookup(JobLookupStatus.UNAVAILABLE, null);
    }

    public JobLookupStatus getStatus() { return status; }

    public EvaluatorJobResponse getJob() { return job; }
}