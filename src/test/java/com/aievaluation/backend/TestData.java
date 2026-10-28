package com.aievaluation.backend;

import java.time.Instant;
import java.util.UUID;

import com.aievaluation.backend.entity.Evaluation;
import com.aievaluation.backend.entity.Submission;
import com.aievaluation.backend.entity.User;
import com.aievaluation.backend.enums.Paper;
import com.aievaluation.backend.enums.SourceType;
import com.aievaluation.backend.enums.SubmissionStatus;
import com.aievaluation.backend.repository.SubmissionRepository;
import com.aievaluation.backend.repository.UserRepository;
import com.aievaluation.backend.service.EvaluationService;

/** Builds the rows tests need: a user, a submitted answer and its first evaluation attempt. */
public class TestData {

    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final EvaluationService evaluationService;

    public TestData(UserRepository userRepository, SubmissionRepository submissionRepository,
                    EvaluationService evaluationService) {
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
        this.evaluationService = evaluationService;
    }

    public User user() {
        User user = new User();
        user.setEmail("student-" + UUID.randomUUID() + "@test.local");
        user.setName("Test Student");
        user.setPasswordHash("not-a-real-hash");
        return userRepository.save(user);
    }

    public Submission submission(User user) {
        Submission submission = new Submission();
        submission.setUser(user);
        submission.setSourceType(SourceType.TYPED);
        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setPaper(Paper.GS2);
        submission.setMaxMarks(10);
        submission.setWordLimit(150);
        submission.setQuestionText("Discuss the role of the Finance Commission.");
        submission.setAnswerText("A short test answer.");
        submission.setWordCount(4);
        submission.setSubmittedAt(Instant.now());
        return submissionRepository.save(submission);
    }

    /** A user, a submission and evaluation attempt 1 (PENDING). */
    public Evaluation firstAttempt() {
        return evaluationService.createAttempt(submission(user()));
    }
}