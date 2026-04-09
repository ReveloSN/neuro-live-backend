CREATE TABLE IF NOT EXISTS crisis_events (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    state VARCHAR(30) NOT NULL,
    intervention_type VARCHAR(40),
    trigger_bpm REAL,
    trigger_spo2 REAL,
    typing_error_rate REAL,
    typing_dwell_time REAL,
    typing_flight_time REAL,
    typing_error_count INTEGER
);

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

CREATE TABLE IF NOT EXISTS intervention_protocols (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(40) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    light_color VARCHAR(40),
    light_intensity INTEGER,
    audio_track VARCHAR(255),
    audio_volume INTEGER,
    ui_reduction_enabled BOOLEAN,
    ui_theme VARCHAR(80),
    high_contrast_enabled BOOLEAN,
    breathing_enabled BOOLEAN,
    breathing_rhythm INTEGER,
    breathing_cycles INTEGER,
    crisis_event_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_intervention_protocols_crisis_event
        FOREIGN KEY (crisis_event_id) REFERENCES crisis_events(id) ON DELETE CASCADE
);

ALTER TABLE IF EXISTS intervention_protocols
    ADD COLUMN IF NOT EXISTS audio_volume INTEGER;

ALTER TABLE IF EXISTS intervention_protocols
    ADD COLUMN IF NOT EXISTS ui_theme VARCHAR(80);

ALTER TABLE IF EXISTS intervention_protocols
    ADD COLUMN IF NOT EXISTS high_contrast_enabled BOOLEAN;

ALTER TABLE IF EXISTS intervention_protocols
    ADD COLUMN IF NOT EXISTS breathing_rhythm INTEGER;

ALTER TABLE IF EXISTS intervention_protocols
    ADD COLUMN IF NOT EXISTS breathing_cycles INTEGER;

CREATE INDEX IF NOT EXISTS idx_crisis_events_patient_started_at
    ON crisis_events(patient_id, started_at DESC);
