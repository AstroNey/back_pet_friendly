CREATE TABLE import_jobs (
    id            UUID         PRIMARY KEY,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    total         INT          NOT NULL DEFAULT 0,
    imported      INT          NOT NULL DEFAULT 0,
    failed_count  INT          NOT NULL DEFAULT 0,
    errors        TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at  TIMESTAMPTZ
);

CREATE INDEX idx_import_jobs_status ON import_jobs (status);
