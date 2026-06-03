-- Schema H2 para dev local (sem ENUMs do Postgres)
DROP TABLE IF EXISTS historico_lead;
DROP TABLE IF EXISTS comentario_lead;
DROP TABLE IF EXISTS tarefa_lead;
DROP TABLE IF EXISTS lead;
DROP TABLE IF EXISTS usuario;

CREATE TABLE usuario (
    id            VARCHAR(36)  PRIMARY KEY,
    nome          VARCHAR(120) NOT NULL,
    email         VARCHAR(120) NOT NULL UNIQUE,
    senha         VARCHAR(255) NOT NULL,
    perfil        VARCHAR(20)  NOT NULL DEFAULT 'VENDEDOR',
    ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
    receber_lembretes BOOLEAN  NOT NULL DEFAULT TRUE,
    resumo_diario BOOLEAN      NOT NULL DEFAULT FALSE,
    timeline_compacta BOOLEAN  NOT NULL DEFAULT FALSE,
    criado_em     TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE lead (
    id            VARCHAR(36)  PRIMARY KEY,
    nome          VARCHAR(200) NOT NULL,
    email         VARCHAR(200),
    telefone      VARCHAR(30),
    origem        VARCHAR(100),
    campanha      VARCHAR(100),
    mensagem      CLOB,
    jornada       VARCHAR(30),
    status        VARCHAR(30)  NOT NULL DEFAULT 'NOVO',
    lido          BOOLEAN      NOT NULL DEFAULT FALSE,
    vendedor_id   VARCHAR(36)  REFERENCES usuario(id) ON DELETE SET NULL,
    dados_extras  CLOB,
    origem_externa VARCHAR(50),
    lead_externo_id VARCHAR(100),
    proxima_acao  VARCHAR(255),
    proximo_contato_em TIMESTAMP,
    ultima_interacao_em TIMESTAMP NOT NULL DEFAULT NOW(),
    criado_em     TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE comentario_lead (
    id          VARCHAR(36) PRIMARY KEY,
    lead_id     VARCHAR(36) NOT NULL REFERENCES lead(id) ON DELETE CASCADE,
    autor_id    VARCHAR(36) NOT NULL REFERENCES usuario(id),
    texto       CLOB        NOT NULL,
    criado_em   TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE historico_lead (
    id            VARCHAR(36) PRIMARY KEY,
    lead_id       VARCHAR(36) NOT NULL REFERENCES lead(id) ON DELETE CASCADE,
    autor_id      VARCHAR(36) REFERENCES usuario(id) ON DELETE SET NULL,
    campo         VARCHAR(80) NOT NULL,
    tipo_evento   VARCHAR(30) NOT NULL DEFAULT 'ALTERACAO',
    origem_acao   VARCHAR(30) NOT NULL DEFAULT 'PAINEL',
    valor_antes   CLOB,
    valor_depois  CLOB,
    descricao     CLOB        NOT NULL,
    criado_em     TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE tarefa_lead (
    id            VARCHAR(36) PRIMARY KEY,
    lead_id       VARCHAR(36) NOT NULL REFERENCES lead(id) ON DELETE CASCADE,
    autor_id      VARCHAR(36) NOT NULL REFERENCES usuario(id),
    titulo        VARCHAR(160) NOT NULL,
    descricao     CLOB,
    vencimento_em TIMESTAMP,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    concluida_em  TIMESTAMP,
    criado_em     TIMESTAMP NOT NULL DEFAULT NOW()
);
