ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS receber_lembretes BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS resumo_diario BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS timeline_compacta BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE lead
    ADD COLUMN IF NOT EXISTS proxima_acao VARCHAR(255),
    ADD COLUMN IF NOT EXISTS proximo_contato_em TIMESTAMP,
    ADD COLUMN IF NOT EXISTS ultima_interacao_em TIMESTAMP NOT NULL DEFAULT NOW();

UPDATE lead
SET ultima_interacao_em = criado_em
WHERE ultima_interacao_em IS NULL;

ALTER TABLE historico_lead
    ADD COLUMN IF NOT EXISTS tipo_evento VARCHAR(30) NOT NULL DEFAULT 'ALTERACAO',
    ADD COLUMN IF NOT EXISTS origem_acao VARCHAR(30) NOT NULL DEFAULT 'PAINEL';

CREATE TABLE IF NOT EXISTS tarefa_lead (
    id             VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    lead_id        VARCHAR(36) NOT NULL REFERENCES lead(id) ON DELETE CASCADE,
    autor_id       VARCHAR(36) NOT NULL REFERENCES usuario(id),
    titulo         VARCHAR(160) NOT NULL,
    descricao      TEXT,
    vencimento_em  TIMESTAMP,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    concluida_em   TIMESTAMP,
    criado_em      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tarefa_lead ON tarefa_lead(lead_id, status, vencimento_em);
