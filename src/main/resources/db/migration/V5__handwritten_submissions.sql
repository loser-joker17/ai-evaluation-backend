-- OCR_FAILED: OCR gave up; the student can type the answer instead.
ALTER TABLE submissions DROP CONSTRAINT ck_submissions_status;
ALTER TABLE submissions ADD CONSTRAINT ck_submissions_status
    CHECK (status IN ('DRAFT', 'OCR_PENDING', 'OCR_READY', 'OCR_FAILED', 'SUBMITTED'));

-- OCR queue fields (same idea as the evaluation dispatcher).
ALTER TABLE submissions
    ADD COLUMN ocr_attempts INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN next_ocr_at  TIMESTAMPTZ,
    ADD COLUMN ocr_error    VARCHAR(300);

CREATE INDEX idx_submissions_ocr_due ON submissions (next_ocr_at) WHERE status = 'OCR_PENDING';

-- One row per uploaded page. The file itself lives in storage under storage_key.
CREATE TABLE submission_images (
    id            UUID         PRIMARY KEY,
    submission_id UUID         NOT NULL REFERENCES submissions (id) ON DELETE CASCADE,
    page_no       INTEGER      NOT NULL,
    storage_key   VARCHAR(200) NOT NULL,
    content_type  VARCHAR(20)  NOT NULL,
    size_bytes    INTEGER      NOT NULL,
    ocr_text      TEXT,
    created_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_submission_images_page   UNIQUE (submission_id, page_no),
    CONSTRAINT ck_submission_images_page   CHECK (page_no BETWEEN 1 AND 5),
    CONSTRAINT ck_submission_images_type   CHECK (content_type IN ('image/jpeg', 'image/png')),
    CONSTRAINT ck_submission_images_size   CHECK (size_bytes > 0 AND size_bytes <= 10485760)
);