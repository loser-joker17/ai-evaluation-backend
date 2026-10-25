CREATE TABLE submissions (
    id            UUID        PRIMARY KEY,
    user_id       UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    source_type   VARCHAR(20) NOT NULL,
    status        VARCHAR(20) NOT NULL,
    paper         VARCHAR(5)  NOT NULL,
    max_marks     INTEGER     NOT NULL,
    word_limit    INTEGER     NOT NULL,
    question_text TEXT        NOT NULL,
    answer_text   TEXT,
    word_count    INTEGER,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    submitted_at  TIMESTAMPTZ,
    CONSTRAINT ck_submissions_source     CHECK (source_type IN ('TYPED', 'HANDWRITTEN')),
    CONSTRAINT ck_submissions_status     CHECK (status IN ('DRAFT', 'OCR_PENDING', 'OCR_READY', 'SUBMITTED')),
    CONSTRAINT ck_submissions_paper      CHECK (paper IN ('GS1', 'GS2', 'GS3', 'GS4')),
    CONSTRAINT ck_submissions_marks      CHECK (max_marks IN (10, 15, 20)),
    CONSTRAINT ck_submissions_word_limit CHECK (word_limit IN (150, 250)),
    CONSTRAINT ck_submissions_question   CHECK (char_length(question_text) BETWEEN 5 AND 5000),
    CONSTRAINT ck_submissions_answer     CHECK (answer_text IS NULL OR char_length(answer_text) <= 15000),
    -- A submitted answer always has text; handwritten drafts may not have it yet (OCR pending).
    CONSTRAINT ck_submissions_submitted  CHECK (status <> 'SUBMITTED'
                                                OR (answer_text IS NOT NULL AND submitted_at IS NOT NULL))
);

CREATE INDEX idx_submissions_user_created ON submissions (user_id, created_at DESC);

CREATE TABLE usage_daily (
    user_id     UUID    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    usage_date  DATE    NOT NULL,
    submissions INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, usage_date)
);