package com.aievaluation.backend.service.impl;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aievaluation.backend.dto.response.EvaluationResultResponse;
import com.aievaluation.backend.entity.Evaluation;
import com.aievaluation.backend.enums.EvaluationStatus;
import com.aievaluation.backend.exception.ConflictException;
import com.aievaluation.backend.mapper.EvaluationMapper;
import com.aievaluation.backend.mapper.EvaluationResultMapper;
import com.aievaluation.backend.repository.EvaluationRepository;
import com.aievaluation.backend.service.EvaluationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aievaluation.backend.dto.request.CreateSubmissionRequest;
import com.aievaluation.backend.dto.response.PageResponse;
import com.aievaluation.backend.dto.response.SubmissionResponse;
import com.aievaluation.backend.dto.response.SubmissionSummaryResponse;
import com.aievaluation.backend.entity.Submission;
import com.aievaluation.backend.entity.User;
import com.aievaluation.backend.enums.MarksOption;
import com.aievaluation.backend.enums.SourceType;
import com.aievaluation.backend.enums.SubmissionStatus;
import com.aievaluation.backend.exception.BadRequestException;
import com.aievaluation.backend.exception.ResourceNotFoundException;
import com.aievaluation.backend.mapper.SubmissionMapper;
import com.aievaluation.backend.repository.SubmissionRepository;
import com.aievaluation.backend.repository.UserRepository;
import com.aievaluation.backend.service.QuotaService;
import com.aievaluation.backend.service.SubmissionService;
import com.aievaluation.backend.util.WordCounter;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MIN_QUESTION_LENGTH = 5;

    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final QuotaService quotaService;
    private final SubmissionMapper submissionMapper;
    private final EvaluationService evaluationService;
    private final EvaluationRepository evaluationRepository;
    private final EvaluationMapper evaluationMapper;
    private final EvaluationResultMapper evaluationResultMapper;

    public SubmissionServiceImpl(SubmissionRepository submissionRepository, UserRepository userRepository,
                                 QuotaService quotaService, SubmissionMapper submissionMapper,EvaluationMapper evaluationMapper,
                                  EvaluationRepository evaluationRepository, EvaluationService evaluationService,
                                 EvaluationResultMapper evaluationResultMapper) {
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.quotaService = quotaService;
        this.submissionMapper = submissionMapper;
        this.evaluationRepository = evaluationRepository;
        this.evaluationService = evaluationService;
        this.evaluationMapper = evaluationMapper;
        this.evaluationResultMapper = evaluationResultMapper;
    }

    @Override
    @Transactional
    public SubmissionResponse create(UUID userId, CreateSubmissionRequest request) {
        MarksOption marks = MarksOption.fromMarks(request.getMaxMarks());
        if (marks == null) {
            throw new BadRequestException("Marks must be 10, 15 or 20");
        }
        if (!marks.isAllowedFor(request.getPaper())) {
            throw new BadRequestException(
                    "GS1-GS3 questions are 10 or 15 marks; GS4 questions are 10, or 20 for case studies");
        }

        String question = request.getQuestionText().strip();
        String answer = request.getAnswerText().strip();
        if (question.length() < MIN_QUESTION_LENGTH) {
            throw new BadRequestException("Question must be at least 5 characters");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        // Same transaction as the save below: if saving fails, the slot is given back.
        quotaService.consume(user);

        Submission submission = new Submission();
        submission.setUser(user);
        submission.setSourceType(SourceType.TYPED);
        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setPaper(request.getPaper());
        submission.setMaxMarks(marks.getMarks());
        submission.setWordLimit(marks.getWordLimit());
        submission.setQuestionText(question);
        submission.setAnswerText(answer);
        submission.setWordCount(WordCounter.count(answer));
        submission.setSubmittedAt(Instant.now());
        submissionRepository.save(submission);

        // Step 5 will also create the evaluation (job) here, in the same transaction.
        // Same transaction: the submission and its job are saved together or not at all.
        Evaluation evaluation = evaluationService.createAttempt(submission);

        SubmissionResponse response = submissionMapper.toResponse(submission);
        response.setEvaluation(evaluationMapper.toSummary(evaluation));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionResponse getForUser(UUID userId, UUID submissionId) {
        Submission submission = findOwned(userId, submissionId);
        SubmissionResponse response = submissionMapper.toResponse(submission);
        Optional<Evaluation> latest =
                evaluationRepository.findFirstBySubmissionIdOrderByAttemptNoDesc(submission.getId());
        if (latest.isPresent()) {
            response.setEvaluation(evaluationMapper.toSummary(latest.get()));
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluationResultResponse getResult(UUID userId, UUID submissionId) {
        Submission submission = findOwned(userId, submissionId);
        Evaluation latest = evaluationRepository.findFirstBySubmissionIdOrderByAttemptNoDesc(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("No evaluation for this submission"));

        EvaluationStatus status = latest.getStatus();
        if (status == EvaluationStatus.COMPLETED || status == EvaluationStatus.REJECTED) {
            return evaluationResultMapper.toResult(latest, submission);
        }
        if (status == EvaluationStatus.FAILED) {
            throw new ConflictException("This answer could not be evaluated. Please submit it again later.");
        }
        throw new ConflictException("This answer is still being evaluated. Check again in a few minutes.");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubmissionSummaryResponse> listForUser(UUID userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<Submission> result = submissionRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(safePage, safeSize));

        PageResponse<SubmissionSummaryResponse> response = new PageResponse<>();
        Map<UUID, Evaluation> latestBySubmission = new HashMap<>();
        if (!result.getContent().isEmpty()) {
            List<UUID> ids = new ArrayList<>();
            for (Submission submission : result.getContent()) {
                ids.add(submission.getId());
            }
            for (Evaluation evaluation : evaluationRepository.findLatestForSubmissions(ids)) {
                latestBySubmission.put(evaluation.getSubmission().getId(), evaluation);
            }
        }

        for (Submission submission : result.getContent()) {
            SubmissionSummaryResponse summary = submissionMapper.toSummary(submission);
            Evaluation latest = latestBySubmission.get(submission.getId());
            if (latest != null) {
                summary.setEvaluationStatus(latest.getStatus().name());
                if (latest.getStatus() == EvaluationStatus.COMPLETED) {
                    summary.setTotalScore(latest.getTotalScore());
                }
                summary.setEvaluatedAt(latest.getFinishedAt());
            }
            response.getContent().add(summary);
        }

        response.setPage(result.getNumber());
        response.setSize(result.getSize());
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        return response;
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID submissionId) {
        Submission submission = findOwned(userId, submissionId);
        if (evaluationRepository.existsBySubmissionIdAndStatusIn(submissionId, EvaluationStatus.inProgress())) {
            throw new ConflictException("This answer is still being evaluated. Delete it once it finishes.");
        }
        submissionRepository.delete(submission);
    }

    // Someone else's submission gives 404, not 403, so ids of other users' answers are never confirmed.
    private Submission findOwned(UUID userId, UUID submissionId) {
        return submissionRepository.findByIdAndUserId(submissionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));
    }
}