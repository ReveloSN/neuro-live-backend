ALTER TABLE IF EXISTS biometric_telemetry_samples
    ADD COLUMN IF NOT EXISTS sensor_contact BOOLEAN;
