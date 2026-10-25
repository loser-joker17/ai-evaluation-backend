package com.aievaluation.backend.client;

import java.util.UUID;

import com.aievaluation.backend.client.enums.DispatchOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import com.aievaluation.backend.client.dto.EvaluatorInput;
import com.aievaluation.backend.client.dto.EvaluatorJobRequest;
import org.springframework.web.client.RestClientException;
import com.aievaluation.backend.client.dto.EvaluatorJobResponse;
import com.aievaluation.backend.client.dto.JobLookup;

@Component
public class EvaluatorClient {

    private static final Logger log = LoggerFactory.getLogger(EvaluatorClient.class);

    private final RestClient evaluatorRestClient;

    public EvaluatorClient(RestClient evaluatorRestClient) {
        this.evaluatorRestClient = evaluatorRestClient;
    }

    /** Sends one job. Never logs the body: it contains the answer text. */
    public DispatchOutcome submitJob(UUID jobId, EvaluatorInput input) {
        try {
            evaluatorRestClient.post()
                    .uri("/jobs")
                    .body(new EvaluatorJobRequest(jobId.toString(), input))
                    .retrieve()
                    .toBodilessEntity();
            return DispatchOutcome.ACCEPTED;
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 503) {
                return DispatchOutcome.BUSY;
            }
            if (status == 400 || status == 401 || status == 403 || status == 422) {
                log.error("Evaluator rejected job {}: HTTP {}", jobId, status);
                return DispatchOutcome.REJECTED;
            }
            log.warn("Evaluator returned HTTP {} for job {}", status, jobId);
            return DispatchOutcome.UNAVAILABLE;
        } catch (ResourceAccessException ex) {
            log.warn("Evaluator unreachable for job {}: {}", jobId, ex.getClass().getSimpleName());
            return DispatchOutcome.UNAVAILABLE;
        }
    }

    /** Asks the evaluator where a job stands. Never logs the body: it contains the result. */
    public JobLookup getJob(UUID jobId) {
        try {
            EvaluatorJobResponse job = evaluatorRestClient.get()
                    .uri("/jobs/{id}", jobId)
                    .retrieve()
                    .body(EvaluatorJobResponse.class);
            return job == null ? JobLookup.unavailable() : JobLookup.found(job);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 404) {
                return JobLookup.notFound();
            }
            if (status == 401 || status == 403) {
                // Our key is wrong. NOT "lost": the job may be fine, so don't resend it.
                log.error("Evaluator refused status check for job {}: HTTP {}. Check EVALUATOR_INTERNAL_KEY.",
                        jobId, status);
            } else {
                log.warn("Evaluator returned HTTP {} for status of job {}", status, jobId);
            }
            return JobLookup.unavailable();
        } catch (ResourceAccessException ex) {
            log.warn("Evaluator unreachable for status of job {}: {}", jobId, ex.getClass().getSimpleName());
            return JobLookup.unavailable();
        } catch (RestClientException ex) {
            // e.g. a JSON decode error. Only the class name: the message can quote the body.
            log.error("Could not read status of job {}: {}", jobId, ex.getClass().getSimpleName());
            return JobLookup.unavailable();
        }
    }
}