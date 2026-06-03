-- V12: adiciona o novo status EM_OUTRO_ATENDIMENTO ao enum de leads.
-- No-op intencional: a V6 ja converteu a coluna `status` de enum nativo para VARCHAR(30)
-- e dropou o tipo `status_lead`. A coluna agora aceita qualquer string ate 30 chars,
-- e a validacao do valor 'EM_OUTRO_ATENDIMENTO' e' feita no Java via enum StatusLead.
-- Mantido o arquivo para preservar a sequencia de versoes do Flyway.
SELECT 1;
