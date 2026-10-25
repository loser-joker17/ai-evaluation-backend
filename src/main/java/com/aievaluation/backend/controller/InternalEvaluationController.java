package com.aievaluation.backend.controller;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aievaluation.backend.client.dto.EvaluatorJobResponse;
import com.aievaluation.backend.enums.ApplyOutcome;
import com.aievaluation.backend.exception.BadRequestException;
import com.aievaluation.backend.exception.ResourceNotFoundException;
import com.aievaluation.backend.security.InternalKeyVerifier;
import com.aievaluation.backend.service.EvaluationResultService;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Called by the evaluator when a job finishes. Not for browsers: no JWT, internal key only.
 *
 * Status codes matter here: the evaluator retries on 5xx/429 and gives up on any other 4xx.
 * So a duplicate or late callback gets 2xx, never an error.
 */
@RestController
@RequestMapping("/internal/evaluations")
public class InternalEvaluationController {

    private final InternalKeyVerifier internalKeyVerifier;
    private final EvaluationResultService evaluationResultService;
    private final ObjectMapper objectMapper;

    public InternalEvaluationController(InternalKeyVerifier internalKeyVerifier,
                                        EvaluationResultService evaluationResultService,
                                        ObjectMapper objectMapper) {
        this.internalKeyVerifier = internalKeyVerifier;
        this.evaluationResultService = evaluationResultService;
        this.objectMapper = objectMapper;
    }

    // The body is taken as a String so the key is checked before we parse anything.
    @PostMapping(value = "/{jobId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receiveResult(@PathVariable String jobId,
                                              @RequestHeader(value = "X-Internal-Key", required = false) String key,
                                              @RequestBody String body) {
        internalKeyVerifier.verify(key);

        UUID evaluationId = parseId(jobId);
        EvaluatorJobResponse job = parseBody(body);
        if (job.getJobId() != null && !jobId.equalsIgnoreCase(job.getJobId())) {
            throw new BadRequestException("job_id in the body does not match the URL");
        }
        if (job.getStatus() == null) {
            throw new BadRequestException("status is required");
        }

        ApplyOutcome outcome = evaluationResultService.apply(evaluationId, job);
        if (outcome == ApplyOutcome.NOT_FOUND) {
            throw new ResourceNotFoundException("Evaluation not found");
        }
        // APPLIED, ALREADY_FINAL and STILL_RUNNING all mean "received, stop retrying".
        return ResponseEntity.noContent().build();
    }

    private UUID parseId(String jobId) {
        try {
            return UUID.fromString(jobId);
        } catch (IllegalArgumentException ex) {
            // Not one of our ids: 404, so the evaluator stops retrying.
            throw new ResourceNotFoundException("Evaluation not found");
        }
    }

    private EvaluatorJobResponse parseBody(String body) {
        try {
            return objectMapper.readValue(body, EvaluatorJobResponse.class);
        } catch (JacksonException ex) {
            // Never put the body or the parser message in the error: it may contain answer text.
            throw new BadRequestException("Malformed evaluator job JSON");
        }
    }
}