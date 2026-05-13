package com.syntra.controller.api;

import com.syntra.dto.BulkAssignDTO;
import com.syntra.dto.ComentarioDTO;
import com.syntra.dto.LeadFiltroDTO;
import com.syntra.dto.LeadUpdateDTO;
import com.syntra.model.ComentarioLead;
import com.syntra.model.HistoricoLead;
import com.syntra.model.Lead;
import com.syntra.repository.LeadRepository;
import com.syntra.service.LeadService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leads")
public class LeadApiController {

    private final LeadService leadService;
    private final LeadRepository leadRepo;

    public LeadApiController(LeadService leadService, LeadRepository leadRepo) {
        this.leadService = leadService;
        this.leadRepo = leadRepo;
    }

    @GetMapping("/nao-lidos")
    public ResponseEntity<Map<String, Long>> naoLidos() {
        return ResponseEntity.ok(Map.of("total", leadRepo.countByLidoFalse()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> atualizar(
            @PathVariable String id,
            @Valid @RequestBody LeadUpdateDTO dto,
            Authentication auth) {

        Lead lead = leadService.atualizar(id, dto, auth.getName());
        return ResponseEntity.ok(toMap(lead));
    }

    @PostMapping("/{id}/lido")
    public ResponseEntity<Void> marcarLido(@PathVariable String id) {
        leadService.marcarLido(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/comentarios")
    public ResponseEntity<Map<String, Object>> adicionarComentario(
            @PathVariable String id,
            @Valid @RequestBody ComentarioDTO dto,
            Authentication auth) {

        ComentarioLead comentario = leadService.adicionarComentario(id, dto.getTexto(), auth.getName());
        return ResponseEntity.ok(comentarioToMap(comentario));
    }

    @GetMapping("/{id}/comentarios")
    public ResponseEntity<List<Map<String, Object>>> listarComentarios(@PathVariable String id) {
        List<Map<String, Object>> lista = leadService.listarComentarios(id)
                .stream().map(this::comentarioToMap).toList();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}/historico")
    public ResponseEntity<List<Map<String, Object>>> listarHistorico(@PathVariable String id) {
        List<Map<String, Object>> lista = leadService.listarHistorico(id)
                .stream().map(this::historicoToMap).toList();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/export")
    public ResponseEntity<List<Map<String, Object>>> exportar(LeadFiltroDTO filtro) {
        List<Map<String, Object>> lista = leadService.exportar(filtro)
                .stream()
                .map(this::leadToMap)
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/bulk-assign")
    public ResponseEntity<List<Map<String, Object>>> atribuirEmMassa(
            @Valid @RequestBody BulkAssignDTO payload,
            Authentication auth) {
        List<Map<String, Object>> lista = leadService.atribuirEmMassa(
                        payload.getLeadIds(),
                        payload.getVendedorId(),
                        auth.getName())
                .stream()
                .map(this::leadToMap)
                .toList();

        return ResponseEntity.ok(lista);
    }

    private Map<String, Object> leadToMap(Lead lead) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", lead.getId());
        m.put("nome", lead.getNome());
        m.put("email", lead.getEmail());
        m.put("telefone", lead.getTelefone());
        m.put("origem", lead.getOrigem());
        m.put("campanha", lead.getCampanha());
        m.put("status", lead.getStatus().name());
        m.put("statusLabel", lead.getStatus().getLabel());
        m.put("vendedor", lead.getVendedor() != null ? lead.getVendedor().getNome() : null);
        m.put("lido", lead.isLido());
        m.put("criadoEm", lead.getCriadoEm().format(fmt));
        return m;
    }

    private Map<String, Object> toMap(Lead lead) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", lead.getId());
        m.put("nome", lead.getNome());
        m.put("status", lead.getStatus().name());
        m.put("statusLabel", lead.getStatus().getLabel());
        m.put("lido", lead.isLido());
        m.put("vendedor", lead.getVendedor() != null ? lead.getVendedor().getNome() : null);
        return m;
    }

    private Map<String, Object> comentarioToMap(ComentarioLead c) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("texto", c.getTexto());
        m.put("autor", c.getAutor().getNome());
        m.put("criadoEm", c.getCriadoEm().format(fmt));
        return m;
    }

    private Map<String, Object> historicoToMap(HistoricoLead h) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", h.getId());
        m.put("campo", h.getCampo());
        m.put("valorAntes", h.getValorAntes());
        m.put("valorDepois", h.getValorDepois());
        m.put("descricao", h.getDescricao());
        m.put("autor", h.getAutor() != null ? h.getAutor().getNome() : "Sistema");
        m.put("criadoEm", h.getCriadoEm().format(fmt));
        return m;
    }
}
