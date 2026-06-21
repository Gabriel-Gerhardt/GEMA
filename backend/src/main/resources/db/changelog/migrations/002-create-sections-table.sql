--liquibase formatted sql

--changeset gabriel-gerhardt:002-create-sections-table

CREATE TABLE sections (
                          id BIGSERIAL PRIMARY KEY,
                          qrcode_id BIGINT NOT NULL,
                          title VARCHAR(255) NOT NULL,
                          content TEXT NOT NULL,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT fk_section_qrcode
                              FOREIGN KEY (qrcode_id)
                                  REFERENCES qrcodes(id)
                                  ON DELETE CASCADE
);

CREATE INDEX idx_sections_qrcode_id
    ON sections(qrcode_id);
