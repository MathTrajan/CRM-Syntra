-- V11: Corrige timestamps gravados em UTC quando o container do Fly rodava
-- em UTC (antes do fix de TZ em SyntraApplication + fly.toml). Sao Paulo e'
-- UTC-3, entao subtraimos 3 horas de todas as colunas TIMESTAMP que foram
-- preenchidas via LocalDateTime.now() do JVM.
--
-- IRREVERSIVEL: roda apenas uma vez (Flyway controla a versao).

UPDATE usuario
   SET criado_em     = criado_em     - INTERVAL '3 hours',
       atualizado_em = atualizado_em - INTERVAL '3 hours';

UPDATE lead
   SET criado_em           = criado_em           - INTERVAL '3 hours',
       atualizado_em       = atualizado_em       - INTERVAL '3 hours',
       ultima_interacao_em = ultima_interacao_em - INTERVAL '3 hours',
       proximo_contato_em  = proximo_contato_em  - INTERVAL '3 hours'
 WHERE criado_em IS NOT NULL;

UPDATE comentario_lead
   SET criado_em = criado_em - INTERVAL '3 hours';

UPDATE historico_lead
   SET criado_em = criado_em - INTERVAL '3 hours';

UPDATE tarefa_lead
   SET criado_em     = criado_em     - INTERVAL '3 hours',
       vencimento_em = vencimento_em - INTERVAL '3 hours',
       concluida_em  = concluida_em  - INTERVAL '3 hours';
