CREATE TABLE users (
    id               UUID         PRIMARY KEY,
    email            VARCHAR(254) NOT NULL,
    name             VARCHAR(100) NOT NULL,
    password_hash    VARCHAR(100),
    google_sub       VARCHAR(255),
    email_verified   BOOLEAN      NOT NULL DEFAULT FALSE,
    role             VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',
    research_consent BOOLEAN      NOT NULL DEFAULT FALSE,
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    last_login_at    TIMESTAMPTZ,
    CONSTRAINT uk_users_email        UNIQUE (email),
    CONSTRAINT uk_users_google_sub   UNIQUE (google_sub),
    CONSTRAINT ck_users_login_method CHECK (password_hash IS NOT NULL OR google_sub IS NOT NULL),
    CONSTRAINT ck_users_role         CHECK (role IN ('STUDENT', 'ADMIN')),
    CONSTRAINT ck_users_email_lower  CHECK (email = LOWER(email))
);

CREATE TABLE refresh_tokens (
    id          UUID        PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);