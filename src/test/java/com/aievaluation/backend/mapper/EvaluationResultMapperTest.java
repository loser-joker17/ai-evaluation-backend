package com.aievaluation.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.aievaluation.backend.dto.response.DimensionResultResponse;
import com.aievaluation.backend.dto.response.EvaluationResultResponse;
import com.aievaluation.backend.entity.Evaluation;
import com.aievaluation.backend.entity.Submission;
import com.aievaluation.backend.enums.EvaluationStatus;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** What the student sees. Plain unit test: no Spring, no database. */
class EvaluationResultMapperTest {

    private static final String RESULT_JSON = """
            {
              "status": "success",
              "total_score": 9.99,
              "raw_total_score": 5.1,
              "max_marks": 10,
              "word_limit_status": "within_limit",
              "feedback_consistent": true,
              "consistency_issues": ["internal note"],
              "dimension_scores": [
                {"dimension": "relevance", "score": 0.84, "max_score": 2.0,
                 "raw_level": 5, "final_level": 3,
                 "cap_note": "internal cap reason", "audit_note": "internal audit",
                 "justification": "Level lowered from 5 to 3: The asked part gets little space.",
                 "evidence_from_answer": ["quote one", ""],
                 "improvement": "Spend more words on the asked part."},
                {"dimension": "content_substantiation", "score": 1.15, "max_score": 2.5,
                 "raw_level": 4, "final_level": 4,
                 "justification": "Good specifics.", "evidence_from_answer": [], "improvement": ""},
                {"dimension": "something_new", "score": 1.0, "max_score": 2.0,
                 "justification": "x", "improvement": "y"}
              ],
              "strengths": ["Cites Article 280."],
              "weaknesses": ["No examples."],
              "improvement_suggestions": ["Add an example."],
              "overall_feedback": "Solid answer.",
              "claim_verifications": [
                {"claim": "Wrong claim", "label": "contradicted", "explanation": "Official data says otherwise.",
                 "sources": ["https://example.gov.in"]},
                {"claim": "Right claim", "label": "supported", "explanation": "ok"},
                {"claim": "Unclear claim", "label": "unverifiable", "explanation": "?"}
              ],
              "run_info": {"evaluator_version": "2026.10.19", "seconds": 205}
            }
            """;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final EvaluationResultMapper mapper = new EvaluationResultMapper(objectMapper);

    @Test
    void completed_mapsFeedbackAndDimensions() {
        EvaluationResultResponse result = mapper.toResult(evaluation(EvaluationStatus.COMPLETED, RESULT_JSON), submission());

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getWordLimitStatus()).isEqualTo("within_limit");
        assertThat(result.getStrengths()).containsExactly("Cites Article 280.");
        assertThat(result.getWeaknesses()).containsExactly("No examples.");
        assertThat(result.getImprovementSuggestions()).containsExactly("Add an example.");
        assertThat(result.getOverallFeedback()).isEqualTo("Solid answer.");
        assertThat(result.getDimensions()).hasSize(3);

        DimensionResultResponse relevance = result.getDimensions().get(0);
        assertThat(relevance.getLabel()).isEqualTo("Relevance");
        assertThat(relevance.getScore()).isEqualByComparingTo("0.84");
        assertThat(relevance.getMaxScore()).isEqualByComparingTo("2.00");
        assertThat(relevance.getEvidence()).containsExactly("quote one");   // blank quote dropped
    }

    @Test
    void totalScore_comesFromOurColumn_notTheJson() {
        Evaluation evaluation = evaluation(EvaluationStatus.COMPLETED, RESULT_JSON);
        evaluation.setTotalScore(new BigDecimal("4.50"));

        // The JSON says 9.99; the student must see the same score as in their history list.
        assertThat(mapper.toResult(evaluation, submission()).getTotalScore()).isEqualByComparingTo("4.50");
    }

    @Test
    void capPrefix_isRemoved_andEmptyImprovementIsNull() {
        EvaluationResultResponse result = mapper.toResult(evaluation(EvaluationStatus.COMPLETED, RESULT_JSON), submission());

        assertThat(result.getDimensions().get(0).getJustification()).isEqualTo("The asked part gets little space.");
        assertThat(result.getDimensions().get(1).getImprovement()).isNull();
    }

    @Test
    void unknownDimension_usesItsNameAsLabel() {
        EvaluationResultResponse result = mapper.toResult(evaluation(EvaluationStatus.COMPLETED, RESULT_JSON), submission());

        assertThat(result.getDimensions().get(2).getLabel()).isEqualTo("something_new");
    }

    @Test
    void onlyContradictedClaims_areShown() {
        EvaluationResultResponse result = mapper.toResult(evaluation(EvaluationStatus.COMPLETED, RESULT_JSON), submission());

        assertThat(result.getFactualFlags()).hasSize(1);
        assertThat(result.getFactualFlags().get(0).getClaim()).isEqualTo("Wrong claim");
        assertThat(result.getFactualFlags().get(0).getSources()).containsExactly("https://example.gov.in");
    }

    @Test
    void internals_neverReachTheStudent() {
        EvaluationResultResponse result = mapper.toResult(evaluation(EvaluationStatus.COMPLETED, RESULT_JSON), submission());

        // Serialise exactly as the API would, then look for anything internal.
        String json = objectMapper.writeValueAsString(result);
        assertThat(json).doesNotContain(
                "run_info", "runInfo", "evaluator_version", "raw_total_score", "rawTotalScore",
                "cap_note", "capNote", "internal cap reason", "audit_note", "internal audit",
                "raw_level", "final_level", "\"level\"", "feedback_consistent", "consistency_issues",
                "internal note", "Level lowered", "supported", "unverifiable");
    }

    @Test
    void rejected_showsReasonsOnly() {
        String rejected = "{\"status\": \"error\", \"errors\": [\"The answer is empty.\"], \"max_marks\": 10}";

        EvaluationResultResponse result = mapper.toResult(evaluation(EvaluationStatus.REJECTED, rejected), submission());

        assertThat(result.getStatus()).isEqualTo("REJECTED");
        assertThat(result.getRejectionReasons()).containsExactly("The answer is empty.");
        assertThat(result.getDimensions()).isEmpty();
    }

    @Test
    void brokenOrMissingJson_neverCrashes() {
        EvaluationResultResponse broken = mapper.toResult(evaluation(EvaluationStatus.COMPLETED, "{oops"), submission());
        EvaluationResultResponse missing = mapper.toResult(evaluation(EvaluationStatus.COMPLETED, null), submission());

        assertThat(broken.getDimensions()).isEmpty();
        assertThat(missing.getDimensions()).isEmpty();
        assertThat(missing.getMaxMarks()).isEqualTo(10);
    }

    // ------------------------------------------------------------------ helpers

    private static Evaluation evaluation(EvaluationStatus status, String resultJson) {
        Evaluation evaluation = new Evaluation();
        evaluation.setAttemptNo(1);
        evaluation.setStatus(status);
        evaluation.setMaxMarks(10);
        evaluation.setResultJson(resultJson);
        evaluation.setFinishedAt(Instant.now());
        return evaluation;
    }

    private static Submission submission() {
        Submission submission = new Submission();
        submission.setWordCount(146);
        submission.setWordLimit(150);
        return submission;
    }
}