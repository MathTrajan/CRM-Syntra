package com.syntra.controller.api;

import com.syntra.model.ComentarioLead;
import com.syntra.model.Lead;
import com.syntra.model.Usuario;
import com.syntra.model.enums.JornadaLead;
import com.syntra.model.enums.StatusLead;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LeadExportMapTest {

    @Test
    void mapeiaTodosOsCamposComDatasIso() {
        Lead lead = new Lead();
        lead.setNome("Maria");
        lead.setEmail("maria@x.com");
        lead.setTelefone("11999998888");
        lead.setOrigem("Site");
        lead.setCampanha("Black Friday");
        lead.setMensagem("Quero orçamento");
        lead.setJornada(JornadaLead.TELEVENDAS);
        lead.setStatus(StatusLead.EM_ATENDIMENTO);
        Usuario v = new Usuario();
        v.setNome("João Vendedor");
        lead.setVendedor(v);
        lead.setProximaAcao("Ligar amanhã");
        LocalDateTime dt = LocalDateTime.of(2026, 6, 3, 14, 30);
        lead.setProximoContatoEm(dt);
        lead.setUltimaInteracaoEm(dt);
        lead.setCriadoEm(dt);
        lead.setAtualizadoEm(dt);
        lead.setOrigemExterna("DIVIA");
        lead.setDadosExtras("{\"x\":1}");

        Map<String, Object> m = LeadApiController.leadExportToMap(lead);

        assertEquals("Maria", m.get("nome"));
        assertEquals("maria@x.com", m.get("email"));
        assertEquals("João Vendedor", m.get("vendedor"));
        assertEquals(StatusLead.EM_ATENDIMENTO.getLabel(), m.get("statusLabel"));
        assertEquals(JornadaLead.TELEVENDAS.getLabel(), m.get("jornadaLabel"));
        assertEquals("2026-06-03T14:30", m.get("criadoEm"));
        assertEquals(false, m.get("lido"));
        assertEquals("DIVIA", m.get("origemExterna"));
        assertEquals("{\"x\":1}", m.get("dadosExtras"));
    }

    @Test
    void camposNulosViramNull() {
        Lead lead = new Lead();
        lead.setNome("Sem dados");
        lead.setStatus(StatusLead.NOVO);
        lead.setProximoContatoEm(null);

        Map<String, Object> m = LeadApiController.leadExportToMap(lead);

        assertNull(m.get("jornadaLabel"));
        assertNull(m.get("vendedor"));
        assertNull(m.get("anotacoes"));
        assertEquals(StatusLead.NOVO.getLabel(), m.get("statusLabel"));
    }

    @Test
    void concatenaAnotacoesInternas() {
        Lead lead = new Lead();
        lead.setNome("Com notas");
        lead.setStatus(StatusLead.NOVO);
        Usuario autor = new Usuario();
        autor.setNome("Ana");
        ComentarioLead c = new ComentarioLead();
        c.setTexto("Cliente pediu retorno");
        c.setAutor(autor);
        c.setCriadoEm(LocalDateTime.of(2026, 6, 3, 9, 15));
        lead.getComentarios().add(c);

        Map<String, Object> m = LeadApiController.leadExportToMap(lead);

        assertEquals("03/06/2026 09:15 — Ana: Cliente pediu retorno", m.get("anotacoes"));
    }
}
