package com.syntra.controller;

import com.syntra.dto.AtualizarFollowUpDTO;
import com.syntra.dto.CriarLeadManualDTO;
import com.syntra.dto.CriarTarefaLeadDTO;
import com.syntra.dto.DiviaSyncResultDTO;
import com.syntra.dto.LeadFiltroDTO;
import com.syntra.model.Lead;
import com.syntra.model.enums.JornadaLead;
import com.syntra.model.enums.StatusLead;
import com.syntra.repository.UsuarioRepository;
import com.syntra.service.AlertaService;
import com.syntra.service.DiviaLeadIntegrationService;
import com.syntra.service.LeadService;
import com.syntra.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/leads")
public class LeadController {

    private final LeadService leadService;
    private final DiviaLeadIntegrationService diviaLeadIntegrationService;
    private final UsuarioRepository usuarioRepo;
    private final UsuarioService usuarioService;
    private final AlertaService alertaService;

    public LeadController(LeadService leadService,
                          DiviaLeadIntegrationService diviaLeadIntegrationService,
                          UsuarioRepository usuarioRepo,
                          UsuarioService usuarioService,
                          AlertaService alertaService) {
        this.leadService = leadService;
        this.diviaLeadIntegrationService = diviaLeadIntegrationService;
        this.usuarioRepo = usuarioRepo;
        this.usuarioService = usuarioService;
        this.alertaService = alertaService;
    }

    @GetMapping
    public String lista(LeadFiltroDTO filtro, Model model, Authentication authentication) {
        Page<Lead> pagina = leadService.listar(filtro);

        model.addAttribute("leads", pagina.getContent());
        model.addAttribute("totalPaginas", pagina.getTotalPages());
        model.addAttribute("paginaAtual", filtro.getPage());
        model.addAttribute("totalElementos", pagina.getTotalElements());
        model.addAttribute("filtro", filtro);
        model.addAttribute("jornadaList", JornadaLead.values());
        model.addAttribute("statusList", StatusLead.values());
        model.addAttribute("vendedores", usuarioRepo.findByAtivoTrueOrderByNome());
        model.addAttribute("paginaAtiva", "leads");

        // Alertas do vendedor logado para o card "Central de Atenção" na lista
        var usuarioLogado = authentication != null
                ? usuarioService.buscarPorEmail(authentication.getName())
                : null;
        model.addAttribute("alertas", usuarioLogado != null
                ? alertaService.alertasDoVendedor(usuarioLogado.getId())
                : java.util.List.of());

        return "leads/lista";
    }

    @PostMapping("/manual")
    public String criarManual(@Valid @ModelAttribute("novoLead") CriarLeadManualDTO dto,
                              BindingResult bindingResult,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String erros = bindingResult.getFieldErrors().stream()
                    .map(e -> e.getDefaultMessage())
                    .reduce((a, b) -> a + " " + b)
                    .orElse("Erro de validacao.");
            redirectAttributes.addFlashAttribute("erro", erros);
            return "redirect:/leads";
        }

        try {
            Lead lead = leadService.criarManual(dto, authentication.getName());
            redirectAttributes.addFlashAttribute("sucesso",
                    "Lead \"" + lead.getNome() + "\" cadastrado com sucesso.");
            return "redirect:/leads/" + lead.getId();
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
            return "redirect:/leads";
        }
    }

    @PostMapping("/sincronizar")
    public String sincronizarDivia(@ModelAttribute LeadFiltroDTO filtro,
                                   RedirectAttributes redirectAttributes) {
        try {
            DiviaSyncResultDTO resultado = diviaLeadIntegrationService.sincronizar(filtro);
            redirectAttributes.addFlashAttribute(
                    "sucesso",
                    "Sincronizacao concluida: "
                            + resultado.getImportados() + " novo(s), "
                            + resultado.getAtualizados() + " atualizado(s), "
                            + resultado.getRecebidos() + " recebido(s) da Divia."
            );
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }

        return "redirect:/leads" + construirQueryString(filtro);
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable String id, Model model, Authentication authentication) {
        preencherDetalhe(model, id, authentication);
        return "leads/detalhe";
    }

    @PostMapping("/{id}/follow-up")
    public String atualizarFollowUp(@PathVariable String id,
                                    @Valid @ModelAttribute("followUpForm") AtualizarFollowUpDTO dto,
                                    BindingResult bindingResult,
                                    Authentication authentication,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            preencherDetalhe(model, id, authentication);
            return "leads/detalhe";
        }

        leadService.atualizarFollowUp(id, dto, authentication.getName());
        redirectAttributes.addFlashAttribute("sucesso", "Follow-up atualizado com sucesso.");
        return "redirect:/leads/" + id;
    }

    @PostMapping("/{id}/tarefas")
    public String criarTarefa(@PathVariable String id,
                              @Valid @ModelAttribute("tarefaForm") CriarTarefaLeadDTO dto,
                              BindingResult bindingResult,
                              Authentication authentication,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            preencherDetalhe(model, id, authentication);
            return "leads/detalhe";
        }

        leadService.criarTarefa(id, dto, authentication.getName());
        redirectAttributes.addFlashAttribute("sucesso", "Tarefa criada com sucesso.");
        return "redirect:/leads/" + id;
    }

    @PostMapping("/{id}/tarefas/{tarefaId}/concluir")
    public String concluirTarefa(@PathVariable String id,
                                 @PathVariable String tarefaId,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        leadService.concluirTarefa(id, tarefaId, authentication.getName());
        redirectAttributes.addFlashAttribute("sucesso", "Tarefa concluida.");
        return "redirect:/leads/" + id;
    }

    private void preencherDetalhe(Model model, String id, Authentication authentication) {
        Lead lead = leadService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Lead nao encontrado."));

        if (!model.containsAttribute("followUpForm")) {
            AtualizarFollowUpDTO followUp = new AtualizarFollowUpDTO();
            followUp.setProximaAcao(lead.getProximaAcao());
            followUp.setProximoContatoEm(lead.getProximoContatoEm());
            model.addAttribute("followUpForm", followUp);
        }
        if (!model.containsAttribute("tarefaForm")) {
            model.addAttribute("tarefaForm", new CriarTarefaLeadDTO());
        }

        model.addAttribute("lead", lead);
        model.addAttribute("comentarios", leadService.listarComentarios(id));
        model.addAttribute("tarefas", leadService.listarTarefas(id));
        model.addAttribute("timeline", leadService.listarTimeline(id));
        model.addAttribute("jornadaList", JornadaLead.values());
        model.addAttribute("statusList", StatusLead.values());
        model.addAttribute("vendedores", usuarioRepo.findByAtivoTrueOrderByNome());
        model.addAttribute("usuarioAtual", usuarioService.buscarPorEmail(authentication.getName()));
        model.addAttribute("paginaAtiva", "leads");
    }

    private String construirQueryString(LeadFiltroDTO filtro) {
        org.springframework.web.util.UriComponentsBuilder builder =
                org.springframework.web.util.UriComponentsBuilder.newInstance();

        if (filtro.getBusca() != null && !filtro.getBusca().isBlank()) {
            builder.queryParam("busca", filtro.getBusca());
        }
        if (filtro.getStatus() != null) {
            builder.queryParam("status", filtro.getStatus());
        }
        if (filtro.getJornada() != null) {
            builder.queryParam("jornada", filtro.getJornada());
        }
        if (filtro.getVendedorId() != null && !filtro.getVendedorId().isBlank()) {
            builder.queryParam("vendedorId", filtro.getVendedorId());
        }
        if (filtro.getDataInicio() != null) {
            builder.queryParam("dataInicio", filtro.getDataInicio());
        }
        if (filtro.getDataFim() != null) {
            builder.queryParam("dataFim", filtro.getDataFim());
        }
        if (filtro.getPage() > 0) {
            builder.queryParam("page", filtro.getPage());
        }
        if (filtro.getSize() != 50) {
            builder.queryParam("size", filtro.getSize());
        }

        String query = builder.build().getQuery();
        return query == null || query.isBlank() ? "" : "?" + query;
    }
}
