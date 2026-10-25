package com.aievaluation.backend.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.aievaluation.backend.dto.response.DimensionResultResponse;
import com.aievaluation.backend.dto.response.EvaluationResultResponse;
import com.aievaluation.backend.dto.response.FactualFlagResponse;
import com.aievaluation.backend.entity.Evaluation;
import com.aievaluation.backend.entity.Submission;
import com.aievaluation.backend.enums.EvaluationStatus;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Builds the student's view from the stored evaluator JSON. Copies only the fields
 * students should see; a missing field gives an empty value, never an error.
 */
@Component
public class EvaluationResultMapper {

    private static final Logger log = LoggerFactory.getLogger(EvaluationResultMapper.class);
    private static final Pattern CAP_PREFIX =
            Pattern.compile("^Level (?:lowered|raised) from \\d to \\d:\\s*");

    private static final Map<String, String> DIMENSION_LABELS = Map.of(
            "relevance", "Relevance",
            "content_substantiation", "Content & substantiation",
            "analysis_depth", "Depth of analysis",
            "coverage_balance", "Coverage & balance",
            "structure_presentation", "Structure & presentation");

    private final ObjectMapper objectMapper;

    public EvaluationResultMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EvaluationResultResponse toResult(Evaluation evaluation, Submission submission) {
        EvaluationResultResponse response = new EvaluationResultResponse();
        response.setEvaluationId(evaluation.getId());
        response.setSubmissionId(submission.getId());
        response.setAttemptNo(evaluation.getAttemptNo());
        response.setStatus(evaluation.getStatus().name());
        response.setTotalScore(evaluation.getTotalScore());   // from our column, not the JSON
        response.setMaxMarks(evaluation.getMaxMarks());
        response.setWordCount(submission.getWordCount());
        response.setWordLimit(submission.getWordLimit());
        response.setFinishedAt(evaluation.getFinishedAt());

        JsonNode root = parse(evaluation);
        if (root == null) {
            return response;
        }

        if (evaluation.getStatus() == EvaluationStatus.REJECTED) {
            response.setRejectionReasons(strings(root.path("errors")));
            return response;
        }

        response.setWordLimitStatus(text(root.path("word_limit_status")));
        response.setDimensions(dimensions(root.path("dimension_scores")));
        response.setStrengths(strings(root.path("strengths")));
        response.setWeaknesses(strings(root.path("weaknesses")));
        response.setImprovementSuggestions(strings(root.path("improvement_suggestions")));
        response.setOverallFeedback(text(root.path("overall_feedback")));
        response.setFactualFlags(factualFlags(root.path("claim_verifications")));
        return response;
    }

    private JsonNode parse(Evaluation evaluation) {
        if (evaluation.getResultJson() == null) {
            return null;
        }
        try {
            return objectMapper.readTree(evaluation.getResultJson());
        } catch (JacksonException ex) {
            // Only the class name: the message could quote the stored JSON.
            log.error("Stored result for evaluation {} is not valid JSON: {}",
                    evaluation.getId(), ex.getClass().getSimpleName());
            return null;
        }
    }

    private List<DimensionResultResponse> dimensions(JsonNode array) {
        List<DimensionResultResponse> list = new ArrayList<>();
        if (!array.isArray()) {
            return list;
        }
        for (int i = 0; i < array.size(); i++) {
            JsonNode node = array.get(i);
            DimensionResultResponse dimension = new DimensionResultResponse();
            String name = text(node.path("dimension"));
            dimension.setDimension(name);
            dimension.setLabel(name == null ? null : DIMENSION_LABELS.getOrDefault(name, name));
            dimension.setScore(decimal(node.path("score")));
            dimension.setMaxScore(decimal(node.path("max_score")));
            dimension.setJustification(stripCapPrefix(text(node.path("justification"))));
            dimension.setEvidence(strings(node.path("evidence_from_answer")));
            dimension.setImprovement(text(node.path("improvement")));
            list.add(dimension);
        }
        return list;
    }

    private List<FactualFlagResponse> factualFlags(JsonNode array) {
        List<FactualFlagResponse> list = new ArrayList<>();
        if (!array.isArray()) {
            return list;
        }
        for (int i = 0; i < array.size(); i++) {
            JsonNode node = array.get(i);
            if (!"contradicted".equals(text(node.path("label")))) {
                continue;
            }
            FactualFlagResponse flag = new FactualFlagResponse();
            flag.setClaim(text(node.path("claim")));
            flag.setExplanation(text(node.path("explanation")));
            flag.setSources(strings(node.path("sources")));
            list.add(flag);
        }
        return list;
    }

    private static List<String> strings(JsonNode array) {
        List<String> list = new ArrayList<>();
        if (!array.isArray()) {
            return list;
        }
        for (int i = 0; i < array.size(); i++) {
            String value = text(array.get(i));
            if (value != null && !value.isBlank()) {
                list.add(value);
            }
        }
        return list;
    }

    private static String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asString();
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String stripCapPrefix(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = CAP_PREFIX.matcher(value).replaceFirst("");
        return cleaned.isBlank() ? null : cleaned;
    }

    private static BigDecimal decimal(JsonNode node) {
        return node.isNumber() ? node.decimalValue().setScale(2, RoundingMode.HALF_UP) : null;
    }
    /** The dimensions of a stored result, for progress stats. Empty if there's no valid JSON. */
    public List<DimensionResultResponse> dimensionsOf(Evaluation evaluation) {
        JsonNode root = parse(evaluation);
        return root == null ? new ArrayList<>() : dimensions(root.path("dimension_scores"));
    }
}