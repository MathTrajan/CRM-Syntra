-- V6:
--  1) Converte colunas com tipo ENUM nativo do Postgres para VARCHAR,
--     evitando o erro "operator does not exist: status_lead = character varying"
--     que ocorre quando o Hibernate envia os parametros como VARCHAR.
--  2) Limpa todos os dados de exemplo e mantem somente um administrador
--     conforme orientacao operacional. A senha real e' entregue fora-de-banda.

-- 1) Converter enums nativos -> VARCHAR
ALTER TABLE lead    ALTER COLUMN status TYPE VARCHAR(30) USING status::text;
ALTER TABLE usuario ALTER COLUMN perfil TYPE VARCHAR(20) USING perfil::text;

-- Manter o default coerente apos a conversao
ALTER TABLE lead    ALTER COLUMN status SET DEFAULT 'NOVO';
ALTER TABLE usuario ALTER COLUMN perfil SET DEFAULT 'VENDEDOR';

-- Garantir NOT NULL (estavam NOT NULL ja, mas o ALTER TYPE preserva isso)
-- (no-op se ja eram NOT NULL)

-- Remover os tipos enum sem uso (depende de nada agora)
DROP TYPE IF EXISTS status_lead;
DROP TYPE IF EXISTS perfil;

-- 2) Higienizar dados: apaga tudo em cascata e deixa um unico admin
TRUNCATE TABLE
    tarefa_lead,
    historico_lead,
    comentario_lead,
    lead,
    usuario
CASCADE;

INSERT INTO usuario (id, nome, email, senha, perfil, ativo)
VALUES (
    gen_random_uuid()::text,
    'Matheus Trajano',
    'matheustrajano.dev@gmail.com',
    '$2b$10$ZHpcM..nyBr3Hv6hRAWTaOtvk7CZ/dPOXxCJIMl5x2srjhSmz91Yq',
    'ADMIN',
    TRUE
);
