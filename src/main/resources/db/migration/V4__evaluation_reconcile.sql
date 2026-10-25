-- How many times the evaluator lost this job (restart) and we sent it again. After 3: FAILED LOST.
ALTER TABLE evaluations ADD COLUMN lost_count INTEGER NOT NULL DEFAULT 0;

-- The reconciler looks for in-flight jobs we haven't checked recently.
-- updated_at changes on every write, so last_checked_at is the right column.
DROP INDEX idx_evaluations_inflight;
CREATE INDEX idx_evaluations_inflight ON evaluations (last_checked_at) WHERE status IN ('QUEUED', 'RUNNING');