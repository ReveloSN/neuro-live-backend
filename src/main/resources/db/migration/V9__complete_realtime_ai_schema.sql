CREATE TABLE IF NOT EXISTS baselines (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL UNIQUE,
    avg_bpm REAL NOT NULL DEFAULT 0,
    avg_spo2 REAL NOT NULL DEFAULT 0,
    calculated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS biometric_telemetry_samples (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    device_mac VARCHAR(17) NOT NULL,
    bpm REAL NOT NULL,
    spo2 REAL NOT NULL,
    observed_at TIMESTAMP NOT NULL,
    prediction_state VARCHAR(30),
    prediction_confidence REAL,
    prediction_reasoning VARCHAR(512)
);

CREATE TABLE IF NOT EXISTS keystroke_dynamics (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    dwell_time REAL NOT NULL,
    flight_time REAL NOT NULL,
    session_id VARCHAR(120),
    error_count INTEGER,
    error_rate REAL,
    timestamp TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS sam_responses (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    crisis_event_id BIGINT NOT NULL UNIQUE,
    valence INTEGER NOT NULL,
    arousal INTEGER NOT NULL,
    recorded_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_sam_responses_crisis_event
        FOREIGN KEY (crisis_event_id) REFERENCES crisis_events(id) ON DELETE CASCADE
);

ALTER TABLE IF EXISTS devices
    ADD COLUMN IF NOT EXISTS device_token_hash VARCHAR(64);

ALTER TABLE IF EXISTS crisis_events
    ADD COLUMN IF NOT EXISTS clinical_summary VARCHAR(4000);

ALTER TABLE IF EXISTS crisis_events
    ADD COLUMN IF NOT EXISTS clinical_summary_generated_at TIMESTAMP;

ALTER TABLE IF EXISTS crisis_events
    ADD COLUMN IF NOT EXISTS clinical_summary_model VARCHAR(80);

ALTER TABLE IF EXISTS biometric_telemetry_samples
    ADD COLUMN IF NOT EXISTS prediction_state VARCHAR(30);

ALTER TABLE IF EXISTS biometric_telemetry_samples
    ADD COLUMN IF NOT EXISTS prediction_confidence REAL;

ALTER TABLE IF EXISTS biometric_telemetry_samples
    ADD COLUMN IF NOT EXISTS prediction_reasoning VARCHAR(512);

CREATE INDEX IF NOT EXISTS idx_baselines_patient_id
    ON baselines(patient_id);

CREATE INDEX IF NOT EXISTS idx_biometric_samples_patient_observed_at
    ON biometric_telemetry_samples(patient_id, observed_at);

CREATE INDEX IF NOT EXISTS idx_keystroke_dynamics_user_timestamp
    ON keystroke_dynamics(user_id, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_sam_responses_patient_recorded_at
    ON sam_responses(patient_id, recorded_at DESC);
