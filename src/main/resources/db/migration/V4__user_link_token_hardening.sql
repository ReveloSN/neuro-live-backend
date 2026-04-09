ALTER TABLE IF EXISTS user_links
    ALTER COLUMN linked_user_id DROP NOT NULL;

ALTER TABLE IF EXISTS user_links
    ALTER COLUMN link_type DROP NOT NULL;

ALTER TABLE IF EXISTS user_links
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;

ALTER TABLE IF EXISTS user_links
    ADD COLUMN IF NOT EXISTS consumed_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_user_links_token_status
    ON user_links(token, status);

CREATE INDEX IF NOT EXISTS idx_user_links_linked_user_status
    ON user_links(linked_user_id, status);
