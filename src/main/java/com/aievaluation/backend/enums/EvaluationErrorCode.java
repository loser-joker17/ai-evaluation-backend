package com.aievaluation.backend.enums;

public enum EvaluationErrorCode {
    EVALUATOR_FAILED,   // the evaluator ran and failed (model timeout, provider down)
    TIMEOUT,            // no final result within the time limit
    CONTRACT,           // the evaluator refused our request (401/422): a bug on our side
    LOST                // the evaluator restarted and lost the job too many times
}