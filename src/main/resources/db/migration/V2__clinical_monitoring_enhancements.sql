ALTER TABLE IF EXISTS activation_thresholds
    ADD COLUMN IF NOT EXISTS patient_id BIGINT;

ALTER TABLE IF EXISTS activation_thresholds
    ADD COLUMN IF NOT EXISTS personal_user_id BIGINT;

ALTER TABLE IF EXISTS activation_thresholds
    ADD COLUMN IF NOT EXISTS defined_by_user_id BIGINT;

ALTER TABLE IF EXISTS keystroke_dynamics
    ADD COLUMN IF NOT EXISTS session_id VARCHAR(120);

ALTER TABLE IF EXISTS keystroke_dynamics
    ADD COLUMN IF NOT EXISTS error_count INTEGER;

ALTER TABLE IF EXISTS keystroke_dynamics
    ADD COLUMN IF NOT EXISTS error_rate REAL;

ALTER TABLE IF EXISTS crisis_events
    ADD COLUMN IF NOT EXISTS trigger_bpm REAL;

ALTER TABLE IF EXISTS crisis_events
    ADD COLUMN IF NOT EXISTS trigger_spo2 REAL;

ALTER TABLE IF EXISTS crisis_events
    ADD COLUMN IF NOT EXISTS typing_error_rate REAL;

ALTER TABLE IF EXISTS crisis_events
    ADD COLUMN IF NOT EXISTS typing_dwell_time REAL;

ALTER TABLE IF EXISTS crisis_events
    ADD COLUMN IF NOT EXISTS typing_flight_time REAL;

ALTER TABLE IF EXISTS crisis_events
    ADD COLUMN IF NOT EXISTS typing_error_count INTEGER;

CREATE TABLE IF NOT EXISTS account_recovery_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    email VARCHAR(150) NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_account_recovery_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_activation_thresholds_patient_active
    ON activation_thresholds(patient_id, active, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_activation_thresholds_personal_active
    ON activation_thresholds(personal_user_id, active, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_account_recovery_tokens_email_active
    ON account_recovery_tokens(email, consumed_at, expires_at DESC);
