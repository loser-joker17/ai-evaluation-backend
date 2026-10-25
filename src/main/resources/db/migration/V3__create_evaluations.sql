CREATE TABLE evaluations (
    id                  UUID         PRIMARY KEY,      -- also the evaluator's job_id
    submission_id       UUID         NOT NULL REFERENCES submissions (id) ON DELETE CASCADE,
    attempt_no          INTEGER      NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    error_code          VARCHAR(30),
    error_message       VARCHAR(500),
    dispatch_count      INTEGER      NOT NULL DEFAULT 0,
    next_dispatch_at    TIMESTAMPTZ,
    last_checked_at     TIMESTAMPTZ,
    queued_at           TIMESTAMPTZ,
    started_at          TIMESTAMPTZ,
    finished_at         TIMESTAMPTZ,
    total_score         NUMERIC(5, 2),
    max_marks           INTEGER      NOT NULL,
    evaluator_version   VARCHAR(40),
    feedback_consistent BOOLEAN,
    result_json         JSONB,
    run_info            JSONB,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_evaluations_attempt UNIQUE (submission_id, attempt_no),
    CONSTRAINT ck_evaluations_status  CHECK (status IN
        ('PENDING', 'QUEUED', 'RUNNING', 'COMPLETED', 'REJECTED', 'FAILED'))
);

-- The dispatcher looks for PENDING rows that are due; the reconciler for jobs in flight.
CREATE INDEX idx_evaluations_due      ON evaluations (next_dispatch_at) WHERE status = 'PENDING';
CREATE INDEX idx_evaluations_inflight ON evaluations (updated_at) WHERE status IN ('QUEUED', 'RUNNING');