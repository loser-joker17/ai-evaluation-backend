-- "Was this feedback useful?" from the student, one answer per evaluation (they can change it).
ALTER TABLE evaluations
    ADD COLUMN feedback_useful  BOOLEAN,
    ADD COLUMN feedback_comment VARCHAR(500),
    ADD COLUMN feedback_at      TIMESTAMPTZ;