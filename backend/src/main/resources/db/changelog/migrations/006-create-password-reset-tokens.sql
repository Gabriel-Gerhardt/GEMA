--liquibase formatted sql

--changeset gabriel-gerhardt:006-create-password-reset-tokens
-- Losing the password meant losing the account and every plan on it, with no
-- way back. This table backs the reset flow.
--
-- Only a SHA-256 hash of the token is stored, never the token itself, for the
-- same reason passwords are hashed: whoever reads this table must not come away
-- able to take over accounts. The hash is unique so a lookup is a single
-- indexed probe rather than a scan.
CREATE TABLE password_reset_tokens (
                       id BIGSERIAL PRIMARY KEY,
                       user_id BIGINT NOT NULL,
                       token_hash VARCHAR(64) NOT NULL UNIQUE,
                       expires_at TIMESTAMP NOT NULL,
                       used_at TIMESTAMP,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT fk_password_reset_user
                           FOREIGN KEY (user_id)
                               REFERENCES users(id)
                               ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_tokens_user_id
    ON password_reset_tokens(user_id);
