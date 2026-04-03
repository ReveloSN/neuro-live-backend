CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    photo_url TEXT,
    CONSTRAINT chk_users_role
        CHECK (role IN ('USER_PERSONAL', 'PATIENT', 'CAREGIVER', 'DOCTOR'))
);

CREATE TABLE IF NOT EXISTS activation_thresholds (
    id BIGSERIAL PRIMARY KEY,
    bpm_min REAL,
    bpm_max REAL,
    spo2_min REAL,
    error_rate_max REAL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS personal_users (
    id BIGINT PRIMARY KEY,
    custom_threshold_id BIGINT UNIQUE,
    CONSTRAINT fk_personal_users_user
        FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_personal_users_threshold
        FOREIGN KEY (custom_threshold_id) REFERENCES activation_thresholds(id)
);

CREATE TABLE IF NOT EXISTS patients (
    id BIGINT PRIMARY KEY,
    consent_given BOOLEAN NOT NULL DEFAULT FALSE,
    consent_date TIMESTAMP,
    CONSTRAINT fk_patients_user
        FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS caregivers (
    id BIGINT PRIMARY KEY,
    CONSTRAINT fk_caregivers_user
        FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS doctors (
    id BIGINT PRIMARY KEY,
    specialty VARCHAR(120),
    CONSTRAINT fk_doctors_user
        FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_links (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    linked_user_id BIGINT NOT NULL,
    link_type VARCHAR(20) NOT NULL,
    token VARCHAR(32) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_links_patient
        FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_links_user
        FOREIGN KEY (linked_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_user_links_type
        CHECK (link_type IN ('CAREGIVER', 'DOCTOR')),
    CONSTRAINT chk_user_links_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'REVOKED'))
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action VARCHAR(150) NOT NULL,
    target_patient_id BIGINT NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_origin VARCHAR(60) NOT NULL,
    CONSTRAINT fk_audit_logs_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_audit_logs_patient
        FOREIGN KEY (target_patient_id) REFERENCES patients(id)
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_user_links_patient_status ON user_links(patient_id, status);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_timestamp ON audit_logs(user_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_patient_timestamp ON audit_logs(target_patient_id, timestamp DESC);
