--liquibase formatted sql

--changeset gabriel-gerhardt:001-initial-schema

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(255) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(100) NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE qrcodes (
                         id BIGSERIAL PRIMARY KEY,
                         public_id VARCHAR(255) NOT NULL UNIQUE,
                         title VARCHAR(255) NOT NULL,
                         is_active BOOLEAN NOT NULL DEFAULT TRUE,
                         content TEXT,
                         user_id BIGINT NOT NULL,
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT fk_qrcode_user
                             FOREIGN KEY (user_id)
                                 REFERENCES users(id)
                                 ON DELETE CASCADE
);

CREATE INDEX idx_qrcodes_public_id
    ON qrcodes(public_id);

CREATE INDEX idx_qrcodes_user_id
    ON qrcodes(user_id);