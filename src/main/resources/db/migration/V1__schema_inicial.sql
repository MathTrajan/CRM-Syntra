-- Enum de perfil do usuário
CREATE TYPE perfil AS ENUM ('ADMIN', 'VENDEDOR');

-- Enum de status do lead
CREATE TYPE status_lead AS ENUM (
    'NOVO',
    'EM_ATENDIMENTO',
    'AGUARDANDO_RETORNO',
    'CONVERTIDO',
    'PERDIDO'
);

-- Tabela de usuários (vendedores e admins)
CREATE TABLE usuario (
    id          VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    nome        VARCHAR(120) NOT NULL,
    email       VARCHAR(120) NOT NULL UNIQUE,
    senha       VARCHAR(255) NOT NULL,
    perfil      perfil       NOT NULL DEFAULT 'VENDEDOR',
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em   TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP  NOT NULL DEFAULT NOW()
);

-- Tabela de leads
CREATE TABLE lead (
    id            VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    nome          VARCHAR(200) NOT NULL,
    email         VARCHAR(200),
    telefone      VARCHAR(30),
    origem        VARCHAR(100),
    campanha      VARCHAR(100),
    mensagem      TEXT,
    status        status_lead  NOT NULL DEFAULT 'NOVO',
    lido          BOOLEAN      NOT NULL DEFAULT FALSE,
    vendedor_id   VARCHAR(36)  REFERENCES usuario(id) ON DELETE SET NULL,
    dados_extras  JSONB,
    criado_em     TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Tabela de comentários internos
CREATE TABLE comentario_lead (
    id          VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    lead_id     VARCHAR(36)  NOT NULL REFERENCES lead(id) ON DELETE CASCADE,
    autor_id    VARCHAR(36)  NOT NULL REFERENCES usuario(id),
    texto       TEXT         NOT NULL,
    criado_em   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Tabela de histórico de movimentações
CREATE TABLE historico_lead (
    id            VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    lead_id       VARCHAR(36)  NOT NULL REFERENCES lead(id) ON DELETE CASCADE,
    autor_id      VARCHAR(36)  REFERENCES usuario(id) ON DELETE SET NULL,
    campo         VARCHAR(80)  NOT NULL,
    valor_antes   TEXT,
    valor_depois  TEXT,
    descricao     TEXT         NOT NULL,
    criado_em     TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Índices para melhorar performance das queries
CREATE INDEX idx_lead_status      ON lead(status);
CREATE INDEX idx_lead_vendedor    ON lead(vendedor_id);
CREATE INDEX idx_lead_criado_em   ON lead(criado_em DESC);
CREATE INDEX idx_lead_lido        ON lead(lido) WHERE lido = FALSE;
CREATE INDEX idx_historico_lead   ON historico_lead(lead_id, criado_em DESC);
CREATE INDEX idx_comentario_lead  ON comentario_lead(lead_id, criado_em DESC);
