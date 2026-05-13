-- V7: A entity Lead declara `dadosExtras` como TEXT, mas a coluna foi
--     criada em V1 como JSONB. O DiviaLeadIntegrationService serializa
--     o objeto via Jackson para String e o Hibernate envia VARCHAR, o
--     que o Postgres recusa contra JSONB. Convertendo a coluna para TEXT
--     resolve sem perder os dados existentes.

ALTER TABLE lead ALTER COLUMN dados_extras TYPE TEXT USING dados_extras::text;
