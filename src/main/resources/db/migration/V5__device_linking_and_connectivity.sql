CREATE TABLE IF NOT EXISTS devices (
    id BIGSERIAL PRIMARY KEY,
    mac_address VARCHAR(17) NOT NULL UNIQUE,
    patient_id BIGINT NOT NULL,
    is_connected BOOLEAN NOT NULL DEFAULT FALSE,
    last_connection TIMESTAMP,
    linked_at TIMESTAMP,
    sensor_contact BOOLEAN,
    fall_back_config VARCHAR(2048),
    CONSTRAINT fk_devices_patient
        FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

ALTER TABLE IF EXISTS devices
    ADD COLUMN IF NOT EXISTS linked_at TIMESTAMP;

ALTER TABLE IF EXISTS devices
    ADD COLUMN IF NOT EXISTS sensor_contact BOOLEAN;

UPDATE devices
SET linked_at = COALESCE(linked_at, last_connection, CURRENT_TIMESTAMP)
WHERE linked_at IS NULL;

UPDATE devices
SET sensor_contact = TRUE
WHERE sensor_contact IS NULL;

CREATE INDEX IF NOT EXISTS idx_devices_patient_id
    ON devices(patient_id);

CREATE INDEX IF NOT EXISTS idx_devices_connected_last_connection
    ON devices(is_connected, last_connection DESC);
