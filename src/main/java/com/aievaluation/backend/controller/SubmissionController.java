package com.aievaluation.backend.controller;

import java.util.UUID;

import com.aievaluation.backend.dto.request.FeedbackRequest;
import com.aievaluation.backend.dto.response.EvaluationResultResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.aievaluation.backend.dto.request.CreateSubmissionRequest;
import com.aievaluation.backend.dto.response.PageResponse;
import com.aievaluation.backend.dto.response.SubmissionResponse;
import com.aievaluation.backend.dto.response.SubmissionSummaryResponse;
import com.aievaluation.backend.security.CurrentUser;
import com.aievaluation.backend.service.SubmissionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    public ResponseEntity<SubmissionResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                     @Valid @RequestBody CreateSubmissionRequest request) {
        SubmissionResponse response = submissionService.create(CurrentUser.id(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public PageResponse<SubmissionSummaryResponse> list(@AuthenticationPrincipal Jwt jwt,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return submissionService.listForUser(CurrentUser.id(jwt), page, size);
    }

    @GetMapping("/{id}")
    public SubmissionResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return submissionService.getForUser(CurrentUser.id(jwt), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        submissionService.delete(CurrentUser.id(jwt), id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{id}/result")
    public EvaluationResultResponse result(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return submissionService.getResult(CurrentUser.id(jwt), id);
    }
    @PostMapping("/{id}/retry")
    public SubmissionResponse retry(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return submissionService.retry(CurrentUser.id(jwt), id);
    }
    @PutMapping("/{id}/feedback")
    public ResponseEntity<Void> feedback(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                         @Valid @RequestBody FeedbackRequest request) {
        submissionService.giveFeedback(CurrentUser.id(jwt), id, request);
        return ResponseEntity.noContent().build();
    }
}