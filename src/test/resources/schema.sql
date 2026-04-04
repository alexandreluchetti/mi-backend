-- H2-compatible schema for tests
DROP TABLE IF EXISTS resumo_registro;
DROP TABLE IF EXISTS upload;

CREATE TABLE upload (
    id          UUID PRIMARY KEY,
    status      VARCHAR(30) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE resumo_registro (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    upload_id   UUID NOT NULL,
    registro    VARCHAR(20) NOT NULL,
    total       BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_upload FOREIGN KEY (upload_id) REFERENCES upload(id) ON DELETE CASCADE
);

CREATE INDEX idx_resumo_upload_id ON resumo_registro(upload_id);
