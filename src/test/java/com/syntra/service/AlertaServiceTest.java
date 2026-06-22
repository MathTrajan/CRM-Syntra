package com.syntra.service;

import com.syntra.dto.AlertaLead;
import com.syntra.model.Lead;
import com.syntra.model.enums.SeveridadeAlerta;
import com.syntra.model.enums.StatusLead;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para AlertaService.avaliar().
 * Sem Spring context — valida apenas a lógica pura de negócio.
 */
class AlertaServiceTest {

    /** Instante fixo usado como "agora" em todos os testes. */
    private LocalDateTime agora;

    @BeforeEach
    void setup() {
        // Data fixa para tornar os testes determinísticos
        agora = LocalDateTime.of(2026, 6, 3, 12, 0, 0);
    }

    // ── Método auxiliar ──────────────────────────────────────────────

    /**
     * Cria um Lead mínimo com o status e ultimaInteracaoEm definidos.
     * criadoEm fica em null para forçar que o fallback só seja usado
     * nos testes que o testam explicitamente.
     */
    private Lead leadCom(StatusLead status, long diasAtras) {
        Lead lead = new Lead();
        lead.setNome("Lead Teste");
        lead.setStatus(status);
        lead.setUltimaInteracaoEm(agora.minusDays(diasAtras));
        lead.setCriadoEm(agora.minusDays(diasAtras)); // mesmo valor; fallback não é exercitado
        return lead;
    }

    // ════════════════════════════════════════════════════════════════
    // NOVO
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("NOVO 1 dia → ATENCAO")
    void novoUmDia_deveGerarAtencao() {
        Lead lead = leadCom(StatusLead.NOVO, 1);

        AlertaLead alerta = AlertaService.avaliar(lead, agora);

        assertNotNull(alerta);
        assertEquals(SeveridadeAlerta.ATENCAO, alerta.getSeveridade());
        assertEquals(1L, alerta.getDiasParado());
        // Mensagem deve conter o número de dias
        assertTrue(alerta.getMensagem().contains("1"),
                "Mensagem deve conter o número de dias: " + alerta.getMensagem());
    }

    @Test
    @DisplayName("NOVO 2 dias → URGENTE")
    void novoDoisDias_deveGerarUrgente() {
        Lead lead = leadCom(StatusLead.NOVO, 2);

        AlertaLead alerta = AlertaService.avaliar(lead, agora);

        assertNotNull(alerta);
        assertEquals(SeveridadeAlerta.URGENTE, alerta.getSeveridade());
        assertEquals(2L, alerta.getDiasParado());
        assertTrue(alerta.getMensagem().contains("2"),
                "Mensagem deve conter o número de dias: " + alerta.getMensagem());
    }

    @Test
    @DisplayName("NOVO 0 dias → sem alerta (abaixo do limiar)")
    void novoZeroDias_semAlerta() {
        Lead lead = leadCom(StatusLead.NOVO, 0);

        assertNull(AlertaService.avaliar(lead, agora));
    }

    // ════════════════════════════════════════════════════════════════
    // EM_ATENDIMENTO
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("EM_ATENDIMENTO 4 dias → ATENCAO")
    void emAtendimento4Dias_deveGerarAtencao() {
        Lead lead = leadCom(StatusLead.EM_ATENDIMENTO, 4);

        AlertaLead alerta = AlertaService.avaliar(lead, agora);

        assertNotNull(alerta);
        assertEquals(SeveridadeAlerta.ATENCAO, alerta.getSeveridade());
        assertEquals(4L, alerta.getDiasParado());
        assertTrue(alerta.getMensagem().contains("4"),
                "Mensagem deve conter o número de dias: " + alerta.getMensagem());
    }

    @Test
    @DisplayName("EM_ATENDIMENTO 7 dias → URGENTE")
    void emAtendimento7Dias_deveGerarUrgente() {
        Lead lead = leadCom(StatusLead.EM_ATENDIMENTO, 7);

        AlertaLead alerta = AlertaService.avaliar(lead, agora);

        assertNotNull(alerta);
        assertEquals(SeveridadeAlerta.URGENTE, alerta.getSeveridade());
        assertEquals(7L, alerta.getDiasParado());
        assertTrue(alerta.getMensagem().contains("7"),
                "Mensagem deve conter o número de dias: " + alerta.getMensagem());
    }

    @Test
    @DisplayName("EM_ATENDIMENTO 3 dias → sem alerta (abaixo do limiar)")
    void emAtendimento3Dias_semAlerta() {
        Lead lead = leadCom(StatusLead.EM_ATENDIMENTO, 3);

        assertNull(AlertaService.avaliar(lead, agora));
    }

    // ════════════════════════════════════════════════════════════════
    // AGUARDANDO_RETORNO
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("AGUARDANDO_RETORNO 7 dias → ATENCAO")
    void aguardando7Dias_deveGerarAtencao() {
        Lead lead = leadCom(StatusLead.AGUARDANDO_RETORNO, 7);

        AlertaLead alerta = AlertaService.avaliar(lead, agora);

        assertNotNull(alerta);
        assertEquals(SeveridadeAlerta.ATENCAO, alerta.getSeveridade());
        assertEquals(7L, alerta.getDiasParado());
        assertTrue(alerta.getMensagem().contains("7"),
                "Mensagem deve conter o número de dias: " + alerta.getMensagem());
    }

    @Test
    @DisplayName("AGUARDANDO_RETORNO 14 dias → URGENTE")
    void aguardando14Dias_deveGerarUrgente() {
        Lead lead = leadCom(StatusLead.AGUARDANDO_RETORNO, 14);

        AlertaLead alerta = AlertaService.avaliar(lead, agora);

        assertNotNull(alerta);
        assertEquals(SeveridadeAlerta.URGENTE, alerta.getSeveridade());
        assertEquals(14L, alerta.getDiasParado());
        assertTrue(alerta.getMensagem().contains("14"),
                "Mensagem deve conter o número de dias: " + alerta.getMensagem());
    }

    // ════════════════════════════════════════════════════════════════
    // CADASTRADO_NO_SITE
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("CADASTRADO_NO_SITE 3 dias → ATENCAO")
    void cadastrado3Dias_deveGerarAtencao() {
        Lead lead = leadCom(StatusLead.CADASTRADO_NO_SITE, 3);

        AlertaLead alerta = AlertaService.avaliar(lead, agora);

        assertNotNull(alerta);
        assertEquals(SeveridadeAlerta.ATENCAO, alerta.getSeveridade());
        assertEquals(3L, alerta.getDiasParado());
        assertTrue(alerta.getMensagem().contains("3"),
                "Mensagem deve conter o número de dias: " + alerta.getMensagem());
    }

    @Test
    @DisplayName("CADASTRADO_NO_SITE 6 dias → URGENTE")
    void cadastrado6Dias_deveGerarUrgente() {
        Lead lead = leadCom(StatusLead.CADASTRADO_NO_SITE, 6);

        AlertaLead alerta = AlertaService.avaliar(lead, agora);

        assertNotNull(alerta);
        assertEquals(SeveridadeAlerta.URGENTE, alerta.getSeveridade());
        assertEquals(6L, alerta.getDiasParado());
        assertTrue(alerta.getMensagem().contains("6"),
                "Mensagem deve conter o número de dias: " + alerta.getMensagem());
    }

    // ════════════════════════════════════════════════════════════════
    // Statuses sem alerta — EM_OUTRO_ATENDIMENTO e CONVERTIDO e PERDIDO
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("EM_OUTRO_ATENDIMENTO → sem alerta (status ignorado)")
    void emOutroAtendimento_semAlerta() {
        Lead lead = leadCom(StatusLead.EM_OUTRO_ATENDIMENTO, 30);

        assertNull(AlertaService.avaliar(lead, agora));
    }

    @Test
    @DisplayName("CONVERTIDO → sem alerta")
    void convertido_semAlerta() {
        Lead lead = leadCom(StatusLead.CONVERTIDO, 30);

        assertNull(AlertaService.avaliar(lead, agora));
    }

    @Test
    @DisplayName("PERDIDO → sem alerta")
    void perdido_semAlerta() {
        Lead lead = leadCom(StatusLead.PERDIDO, 30);

        assertNull(AlertaService.avaliar(lead, agora));
    }

    // ════════════════════════════════════════════════════════════════
    // Fallback para criadoEm quando ultimaInteracaoEm é null
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("ultimaInteracaoEm null → usa criadoEm como fallback")
    void fallbackParaCriadoEm_deveCalcularDiasCorretamente() {
        Lead lead = new Lead();
        lead.setNome("Lead Fallback");
        lead.setStatus(StatusLead.NOVO);
        lead.setUltimaInteracaoEm(null);                  // força fallback
        lead.setCriadoEm(agora.minusDays(2));             // 2 dias atrás → URGENTE

        AlertaLead alerta = AlertaService.avaliar(lead, agora);

        assertNotNull(alerta);
        assertEquals(SeveridadeAlerta.URGENTE, alerta.getSeveridade());
        assertEquals(2L, alerta.getDiasParado());
        assertTrue(alerta.getMensagem().contains("2"),
                "Mensagem deve conter o número de dias: " + alerta.getMensagem());
    }
}
