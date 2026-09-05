ALTER TABLE refresh_tokens
    ADD COLUMN family_id UUID NOT NULL,
    ADD COLUMN replaced_by UUID;

CREATE INDEX idx_refresh_tokens_family_id
    ON refresh_tokens(family_id);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_token_replaced_by
    FOREIGN KEY (replaced_by)
    REFERENCES refresh_tokens(id);