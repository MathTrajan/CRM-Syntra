-- V10: Remove o status CAMPANHA_NA_OMNI do enum StatusLead.
-- Leads existentes nesse status sao migrados para EM_ATENDIMENTO para
-- preservar a continuidade do funil (nao foram convertidos nem perdidos).
UPDATE lead
   SET status = 'EM_ATENDIMENTO'
 WHERE status = 'CAMPANHA_NA_OMNI';
