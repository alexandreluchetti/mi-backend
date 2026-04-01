-- V1__init.sql – Schema inicial do mi-backend

CREATE TABLE IF NOT EXISTS upload (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status      VARCHAR(30) NOT NULL DEFAULT 'EM_PROCESSAMENTO',
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS resumo_registro (
    id          BIGSERIAL PRIMARY KEY,
    upload_id   UUID NOT NULL REFERENCES upload(id) ON DELETE CASCADE,
    registro    VARCHAR(20) NOT NULL,
    total       BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_resumo_upload_id ON resumo_registro(upload_id);
