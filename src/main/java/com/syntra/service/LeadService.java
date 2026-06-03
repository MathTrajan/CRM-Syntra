package com.syntra.service;

import com.syntra.dto.AtualizarFollowUpDTO;
import com.syntra.dto.CriarLeadManualDTO;
import com.syntra.dto.CriarTarefaLeadDTO;
import com.syntra.dto.LeadFiltroDTO;
import com.syntra.dto.LeadUpdateDTO;
import com.syntra.dto.TimelineEntryDTO;
import com.syntra.dto.WebhookPayloadDTO;
import com.syntra.model.ComentarioLead;
import com.syntra.model.HistoricoLead;
import com.syntra.model.Lead;
import com.syntra.model.TarefaLead;
import com.syntra.model.Usuario;
import com.syntra.model.enums.JornadaLead;
import com.syntra.model.enums.Perfil;
import com.syntra.model.enums.StatusLead;
import com.syntra.model.enums.StatusTarefaLead;
import com.syntra.repository.ComentarioLeadRepository;
import com.syntra.repository.HistoricoLeadRepository;
import com.syntra.repository.LeadRepository;
import com.syntra.repository.TarefaLeadRepository;
import com.syntra.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class LeadService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String LISANDRA_ID = "0cad46fb-5795-4683-9135-18b051e6b32b";

    private final LeadRepository leadRepo;
    private final UsuarioRepository usuarioRepo;
    private final ComentarioLeadRepository comentarioRepo;
    private final HistoricoLeadRepository historicoRepo;
    private final TarefaLeadRepository tarefaRepo;

    public LeadService(LeadRepository leadRepo,
                       UsuarioRepository usuarioRepo,
                       ComentarioLeadRepository comentarioRepo,
                       HistoricoLeadRepository historicoRepo,
                       TarefaLeadRepository tarefaRepo) {
        this.leadRepo = leadRepo;
        this.usuarioRepo = usuarioRepo;
        this.comentarioRepo = comentarioRepo;
        this.historicoRepo = historicoRepo;
        this.tarefaRepo = tarefaRepo;
    }

    @Transactional(readOnly = true)
    public Page<Lead> listar(LeadFiltroDTO filtro) {
        Pageable pageable = PageRequest.of(filtro.getPage(), filtro.getSize());
        Filtros f = montarFiltros(filtro);
        return leadRepo.buscar(filtro.getStatus(), filtro.getJornada(), f.semVendedor, f.vendedorId,
                f.busca, f.buscaDigits, f.dataInicio, f.dataFim, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Lead> buscarPorId(String id) {
        return leadRepo.findById(id);
    }

    @Transactional
    public Lead criarViaWebhook(WebhookPayloadDTO payload) {
        LocalDateTime agora = LocalDateTime.now();
        Lead lead = new Lead();
        lead.setNome(payload.getNome());
        lead.setEmail(payload.getEmail());
        lead.setTelefone(payload.getTelefone());
        lead.setOrigem(payload.getOrigem());
        lead.setCampanha(payload.getCampanha());
        lead.setMensagem(payload.getMensagem());
        lead.setDadosExtras(payload.getExtrasJson());
        lead.setUltimaInteracaoEm(agora);

        Optional<Lead> existente = buscarLeadExistenteMesmoCliente(
                payload.getEmail(), payload.getTelefone(), null);

        Usuario vendedorAuto;
        String motivoAtribuicao;
        if (existente.isPresent()) {
            Lead ant = existente.get();
            vendedorAuto = ant.getVendedor();
            if (ant.getJornada() != null) {
                lead.setJornada(ant.getJornada());
            }
            if (ant.getStatus() != null) {
                lead.setStatus(ant.getStatus());
            }
            motivoAtribuicao = vendedorAuto != null
                    ? "Cliente recorrente - replicado vendedor/jornada/status de atendimento anterior"
                    : "Cliente recorrente - replicado jornada/status de atendimento anterior";
        } else {
            vendedorAuto = proximoVendedorRoundRobin();
            motivoAtribuicao = vendedorAuto != null
                    ? "Atribuido automaticamente (round-robin) para: " + vendedorAuto.getNome()
                    : null;
        }
        if (vendedorAuto != null) {
            lead.setVendedor(vendedorAuto);
            aplicarRegraJornadaLisandra(lead);
        }

        lead = leadRepo.save(lead);

        registrarHistorico(lead, null, "lead", "ENTRADA", "WEBHOOK", null, "Novo",
                "Lead recebido via LeadPage", agora);
        if (vendedorAuto != null) {
            registrarHistorico(lead, null, "vendedor", "ALTERACAO", "WEBHOOK",
                    "Sem vendedor", vendedorAuto.getNome(), motivoAtribuicao, agora);
        }

        return lead;
    }

    @Transactional
    public Lead criarManual(CriarLeadManualDTO dto, String autorEmail) {
        Usuario autor = buscarAutorOpcional(autorEmail);
        LocalDateTime agora = LocalDateTime.now();

        Lead lead = new Lead();
        lead.setNome(dto.getNome().trim());
        lead.setEmail(blankToNull(dto.getEmail()));
        lead.setTelefone(blankToNull(dto.getTelefone()));
        lead.setOrigem(blankToNull(dto.getOrigem()));
        lead.setCampanha(blankToNull(dto.getCampanha()));
        lead.setMensagem(blankToNull(dto.getMensagem()));
        lead.setJornada(dto.getJornada());
        lead.setOrigemExterna("MANUAL");
        lead.setLido(true);
        lead.setUltimaInteracaoEm(agora);

        // Jornada Venda Direta => status ja entra como CONVERTIDO
        if (dto.getJornada() == JornadaLead.VENDA_DIRETA) {
            lead.setStatus(StatusLead.CONVERTIDO);
        }

        Usuario vendedor = null;
        String motivoAtribuicao = null;
        if (dto.getVendedorId() != null && !dto.getVendedorId().isBlank()) {
            vendedor = usuarioRepo.findById(dto.getVendedorId())
                    .orElseThrow(() -> new IllegalArgumentException("Vendedor nao encontrado."));
        } else if (isVendedorAtivo(autor)) {
            vendedor = autor;
            motivoAtribuicao = "Atribuido automaticamente ao vendedor que realizou o cadastro.";
        } else {
            Optional<Lead> existente = buscarLeadExistenteMesmoCliente(
                    dto.getEmail(), dto.getTelefone(), null);
            if (existente.isPresent()) {
                Lead ant = existente.get();
                vendedor = ant.getVendedor();
                if (ant.getJornada() != null && lead.getJornada() == null) {
                    lead.setJornada(ant.getJornada());
                }
                if (ant.getStatus() != null && dto.getJornada() != JornadaLead.VENDA_DIRETA) {
                    lead.setStatus(ant.getStatus());
                }
                motivoAtribuicao = vendedor != null
                        ? "Cliente recorrente - replicado vendedor/jornada/status anterior"
                        : "Cliente recorrente - replicado jornada/status anterior";
            } else {
                vendedor = proximoVendedorRoundRobin();
                if (vendedor != null) {
                    motivoAtribuicao = "Atribuido automaticamente (round-robin) para: " + vendedor.getNome();
                }
            }
        }
        if (vendedor != null) {
            lead.setVendedor(vendedor);
            aplicarRegraJornadaLisandra(lead);
        }

        lead = leadRepo.save(lead);

        registrarHistorico(lead, autor, "lead", "ENTRADA", "PAINEL", null, "Novo",
                "Lead cadastrado manualmente", agora);
        if (motivoAtribuicao != null) {
            registrarHistorico(lead, autor, "vendedor", "ALTERACAO", "PAINEL",
                    "Sem vendedor", vendedor.getNome(), motivoAtribuicao, agora);
        }

        return lead;
    }

    @Transactional
    public Lead atualizar(String leadId, LeadUpdateDTO dto, String autorEmail) {
        Lead lead = buscarLeadObrigatorio(leadId);
        Usuario autor = buscarAutorOpcional(autorEmail);
        LocalDateTime eventoEm = LocalDateTime.now();

        boolean jornadaMudou = dto.getJornada() != lead.getJornada();
        if (jornadaMudou) {
            registrarHistorico(lead, autor, "jornada", "ALTERACAO", "PAINEL",
                    labelJornada(lead.getJornada()), labelJornada(dto.getJornada()),
                    "Jornada alterada para: " + labelJornada(dto.getJornada()), eventoEm);
            lead.setJornada(dto.getJornada());
        }

        // Regra de negocio: jornada Venda Direta forca status CONVERTIDO
        StatusLead statusAlvo = dto.getStatus();
        if (dto.getJornada() == JornadaLead.VENDA_DIRETA && lead.getStatus() != StatusLead.CONVERTIDO) {
            statusAlvo = StatusLead.CONVERTIDO;
        }

        if (statusAlvo != null && statusAlvo != lead.getStatus()) {
            registrarHistorico(lead, autor, "status", "ALTERACAO", "PAINEL",
                    lead.getStatus().getLabel(), statusAlvo.getLabel(),
                    "Status alterado para: " + statusAlvo.getLabel(), eventoEm);
            lead.setStatus(statusAlvo);
        }

        if (dto.getVendedorId() != null) {
            String vendedorAntes = lead.getVendedor() != null ? lead.getVendedor().getNome() : "Sem vendedor";
            String vendedorAtualId = lead.getVendedor() != null ? lead.getVendedor().getId() : "";

            if (dto.getVendedorId().isBlank()) {
                if (lead.getVendedor() != null) {
                    lead.setVendedor(null);
                    registrarHistorico(lead, autor, "vendedor", "ALTERACAO", "PAINEL",
                            vendedorAntes, "Sem vendedor", "Vendedor removido", eventoEm);
                }
            } else if (!dto.getVendedorId().equals(vendedorAtualId)) {
                Usuario vendedor = usuarioRepo.findById(dto.getVendedorId())
                        .orElseThrow(() -> new IllegalArgumentException("Vendedor nao encontrado."));
                JornadaLead jornadaAntes = lead.getJornada();
                lead.setVendedor(vendedor);
                aplicarRegraJornadaLisandra(lead);
                registrarHistorico(lead, autor, "vendedor", "ALTERACAO", "PAINEL",
                        vendedorAntes, vendedor.getNome(), "Atribuido para: " + vendedor.getNome(), eventoEm);
                if (isLisandra(vendedor) && jornadaAntes != JornadaLead.TELEVENDAS) {
                    registrarHistorico(lead, autor, "jornada", "ALTERACAO", "PAINEL",
                            labelJornada(jornadaAntes), labelJornada(JornadaLead.TELEVENDAS),
                            "Jornada ajustada automaticamente para: " + labelJornada(JornadaLead.TELEVENDAS),
                            eventoEm);
                }
            }
        }

        lead.setLido(true);
        lead.setUltimaInteracaoEm(eventoEm);
        return leadRepo.save(lead);
    }

    @Transactional(readOnly = true)
    public List<Lead> exportar(LeadFiltroDTO filtro) {
        Pageable pageable = Pageable.unpaged();
        Filtros f = montarFiltros(filtro);
        return leadRepo.buscar(filtro.getStatus(), filtro.getJornada(), f.semVendedor, f.vendedorId,
                        f.busca, f.buscaDigits, f.dataInicio, f.dataFim, pageable)
                .getContent();
    }

    /** Normaliza os parametros opcionais do filtro em uma forma pronta para o repositorio. */
    private Filtros montarFiltros(LeadFiltroDTO filtro) {
        LocalDateTime dataInicio = filtro.getDataInicio() != null
                ? filtro.getDataInicio().atStartOfDay() : null;
        LocalDateTime dataFim = filtro.getDataFim() != null
                ? filtro.getDataFim().atTime(23, 59, 59) : null;

        // Os formularios HTML enviam strings vazias para campos nao preenchidos
        // ("?vendedorId="). Tratamos vazio == null para que a query usa as
        // clausulas IS NULL ao inves de comparar com string vazia (o que
        // sempre falha).
        String vendedorIdRaw = blankToNull(filtro.getVendedorId());
        boolean semVendedor = "sem_vendedor".equals(vendedorIdRaw);
        String vendedorId = semVendedor ? null : vendedorIdRaw;

        String busca = blankToNull(filtro.getBusca());
        if (busca != null) busca = busca.trim();

        // Se a busca tem ao menos 3 digitos, gera versao somente-digitos para casar
        // com telefones formatados como "(11) 99988-7766". Usamos isso como
        // segundo termo de OR para nao restringir a busca textual.
        String buscaDigits = null;
        if (busca != null) {
            String soDigitos = busca.replaceAll("\\D", "");
            if (soDigitos.length() >= 3) {
                buscaDigits = soDigitos;
            }
        }

        return new Filtros(semVendedor, vendedorId, busca, buscaDigits, dataInicio, dataFim);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private record Filtros(boolean semVendedor,
                           String vendedorId,
                           String busca,
                           String buscaDigits,
                           LocalDateTime dataInicio,
                           LocalDateTime dataFim) {}

    @Transactional
    public List<Lead> atribuirEmMassa(List<String> leadIds, String vendedorId, String autorEmail) {
        if (leadIds == null || leadIds.isEmpty()) {
            throw new IllegalArgumentException("Nenhum lead selecionado para atribuicao.");
        }

        // Deduplica e normaliza: o front pode enviar IDs repetidos se o usuario
        // clicar varias vezes, e queremos comparar contra os encontrados sem
        // estourar a checagem.
        List<String> idsUnicos = leadIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .distinct()
                .toList();

        if (idsUnicos.isEmpty()) {
            throw new IllegalArgumentException("Nenhum lead valido selecionado para atribuicao.");
        }

        Usuario autor = buscarAutorOpcional(autorEmail);
        Usuario vendedor = null;

        if (vendedorId != null && !vendedorId.isBlank()) {
            vendedor = usuarioRepo.findById(vendedorId)
                    .orElseThrow(() -> new IllegalArgumentException("Vendedor nao encontrado."));
        }

        List<Lead> leads = leadRepo.findAllById(idsUnicos);
        if (leads.isEmpty()) {
            throw new IllegalArgumentException("Nenhum lead encontrado para os IDs informados.");
        }

        LocalDateTime eventoEm = LocalDateTime.now();
        for (Lead lead : leads) {
            String vendedorAntes = lead.getVendedor() != null ? lead.getVendedor().getNome() : "Sem vendedor";
            String vendedorDepois = vendedor != null ? vendedor.getNome() : "Sem vendedor";

            if (!vendedorDepois.equals(vendedorAntes)) {
                lead.setVendedor(vendedor);
                JornadaLead jornadaAntes = lead.getJornada();
                aplicarRegraJornadaLisandra(lead);
                registrarHistorico(lead, autor, "vendedor", "ALTERACAO", "PAINEL",
                        vendedorAntes, vendedorDepois, "Atribuido em massa para: " + vendedorDepois, eventoEm);
                if (isLisandra(vendedor) && jornadaAntes != JornadaLead.TELEVENDAS) {
                    registrarHistorico(lead, autor, "jornada", "ALTERACAO", "PAINEL",
                            labelJornada(jornadaAntes), labelJornada(JornadaLead.TELEVENDAS),
                            "Jornada ajustada automaticamente para: " + labelJornada(JornadaLead.TELEVENDAS),
                            eventoEm);
                }
            }

            lead.setLido(true);
            lead.setUltimaInteracaoEm(eventoEm);
        }

        return leadRepo.saveAll(leads);
    }

    @Transactional
    public ComentarioLead adicionarComentario(String leadId, String texto, String autorEmail) {
        Lead lead = buscarLeadObrigatorio(leadId);
        Usuario autor = usuarioRepo.findByEmail(autorEmail.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado."));

        ComentarioLead comentario = new ComentarioLead();
        comentario.setLead(lead);
        comentario.setAutor(autor);
        comentario.setTexto(texto.trim());

        ComentarioLead salvo = comentarioRepo.save(comentario);
        lead.setUltimaInteracaoEm(salvo.getCriadoEm());
        lead.setLido(true);
        leadRepo.save(lead);
        return salvo;
    }

    @Transactional
    public Lead atualizarFollowUp(String leadId, AtualizarFollowUpDTO dto, String autorEmail) {
        Lead lead = buscarLeadObrigatorio(leadId);
        Usuario autor = buscarAutorOpcional(autorEmail);
        LocalDateTime eventoEm = LocalDateTime.now();
        boolean mudou = false;

        String proximaAcao = normalizarTexto(dto.getProximaAcao());
        if (!equalsNullable(lead.getProximaAcao(), proximaAcao)) {
            registrarHistorico(lead, autor, "proximaAcao", "FOLLOW_UP", "PAINEL",
                    vazioParaPlaceholder(lead.getProximaAcao()), vazioParaPlaceholder(proximaAcao),
                    "Proxima acao atualizada", eventoEm);
            lead.setProximaAcao(proximaAcao);
            mudou = true;
        }

        if (!equalsNullable(lead.getProximoContatoEm(), dto.getProximoContatoEm())) {
            registrarHistorico(lead, autor, "proximoContatoEm", "FOLLOW_UP", "PAINEL",
                    formatarData(lead.getProximoContatoEm()), formatarData(dto.getProximoContatoEm()),
                    "Proximo contato atualizado", eventoEm);
            lead.setProximoContatoEm(dto.getProximoContatoEm());
            mudou = true;
        }

        if (mudou) {
            lead.setUltimaInteracaoEm(eventoEm);
        }
        lead.setLido(true);
        return leadRepo.save(lead);
    }

    @Transactional
    public TarefaLead criarTarefa(String leadId, CriarTarefaLeadDTO dto, String autorEmail) {
        Lead lead = buscarLeadObrigatorio(leadId);
        Usuario autor = usuarioRepo.findByEmail(autorEmail.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado."));

        TarefaLead tarefa = new TarefaLead();
        tarefa.setLead(lead);
        tarefa.setAutor(autor);
        tarefa.setTitulo(dto.getTitulo().trim());
        tarefa.setDescricao(normalizarTexto(dto.getDescricao()));
        tarefa.setVencimentoEm(dto.getVencimentoEm());

        TarefaLead salva = tarefaRepo.save(tarefa);
        LocalDateTime eventoEm = salva.getCriadoEm();

        registrarHistorico(lead, autor, "tarefa", "TAREFA", "PAINEL", null, salva.getTitulo(),
                descricaoNovaTarefa(salva), eventoEm);

        lead.setUltimaInteracaoEm(eventoEm);
        lead.setLido(true);
        leadRepo.save(lead);
        return salva;
    }

    @Transactional
    public TarefaLead concluirTarefa(String leadId, String tarefaId, String autorEmail) {
        Lead lead = buscarLeadObrigatorio(leadId);
        Usuario autor = buscarAutorOpcional(autorEmail);
        TarefaLead tarefa = tarefaRepo.findByIdAndLeadId(tarefaId, leadId)
                .orElseThrow(() -> new IllegalArgumentException("Tarefa nao encontrada."));

        if (tarefa.getStatus() == StatusTarefaLead.CONCLUIDA) {
            return tarefa;
        }

        LocalDateTime eventoEm = LocalDateTime.now();
        tarefa.setStatus(StatusTarefaLead.CONCLUIDA);
        tarefa.setConcluidaEm(eventoEm);
        tarefa = tarefaRepo.save(tarefa);

        registrarHistorico(lead, autor, "tarefa", "TAREFA", "PAINEL", "Pendente", "Concluida",
                "Tarefa concluida: " + tarefa.getTitulo(), eventoEm);

        lead.setUltimaInteracaoEm(eventoEm);
        lead.setLido(true);
        leadRepo.save(lead);
        return tarefa;
    }

    @Transactional
    public void marcarLido(String leadId) {
        leadRepo.findById(leadId).ifPresent(lead -> {
            lead.setLido(true);
            leadRepo.save(lead);
        });
    }

    @Transactional(readOnly = true)
    public List<ComentarioLead> listarComentarios(String leadId) {
        return comentarioRepo.findByLeadIdOrderByCriadoEmDesc(leadId);
    }

    @Transactional(readOnly = true)
    public List<HistoricoLead> listarHistorico(String leadId) {
        return historicoRepo.findByLeadIdOrderByCriadoEmDesc(leadId);
    }

    @Transactional(readOnly = true)
    public List<TarefaLead> listarTarefas(String leadId) {
        return tarefaRepo.findByLeadIdOrderByStatusAscVencimentoEmAscCriadoEmDesc(leadId);
    }

    @Transactional(readOnly = true)
    public List<TimelineEntryDTO> listarTimeline(String leadId) {
        List<TimelineEntryDTO> historico = listarHistorico(leadId).stream()
                .map(this::historicoParaTimeline)
                .toList();

        List<TimelineEntryDTO> comentarios = listarComentarios(leadId).stream()
                .map(this::comentarioParaTimeline)
                .toList();

        return Stream.concat(historico.stream(), comentarios.stream())
                .sorted(Comparator.comparing(TimelineEntryDTO::getData).reversed())
                .toList();
    }

    private void registrarHistorico(Lead lead,
                                    Usuario autor,
                                    String campo,
                                    String tipoEvento,
                                    String origemAcao,
                                    String antes,
                                    String depois,
                                    String descricao,
                                    LocalDateTime criadoEm) {
        HistoricoLead hist = new HistoricoLead();
        hist.setLead(lead);
        hist.setAutor(autor);
        hist.setCampo(campo);
        hist.setTipoEvento(tipoEvento);
        hist.setOrigemAcao(origemAcao);
        hist.setValorAntes(antes);
        hist.setValorDepois(depois);
        hist.setDescricao(descricao);
        hist.setCriadoEm(criadoEm);
        historicoRepo.save(hist);
    }

    private TimelineEntryDTO historicoParaTimeline(HistoricoLead historico) {
        TimelineEntryDTO item = new TimelineEntryDTO();
        item.setTipo(historico.getTipoEvento());
        item.setTitulo(historico.getDescricao());
        item.setDescricao(historico.getCampo() != null ? "Campo: " + historico.getCampo() : null);
        item.setAutor(historico.getAutor() != null ? historico.getAutor().getNome() : "Sistema");
        item.setData(historico.getCriadoEm());
        item.setOrigem(historico.getOrigemAcao());
        item.setValorAntes(historico.getValorAntes());
        item.setValorDepois(historico.getValorDepois());
        item.setBadge(badgePorTipo(historico.getTipoEvento()));
        return item;
    }

    private TimelineEntryDTO comentarioParaTimeline(ComentarioLead comentario) {
        TimelineEntryDTO item = new TimelineEntryDTO();
        item.setTipo("COMENTARIO");
        item.setTitulo("Comentario interno");
        item.setDescricao(comentario.getTexto());
        item.setAutor(comentario.getAutor().getNome());
        item.setData(comentario.getCriadoEm());
        item.setOrigem("PAINEL");
        item.setBadge("Comentario");
        return item;
    }

    private Lead buscarLeadObrigatorio(String leadId) {
        return leadRepo.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead nao encontrado."));
    }

    private Usuario buscarAutorOpcional(String autorEmail) {
        if (autorEmail == null || autorEmail.isBlank()) {
            return null;
        }
        return usuarioRepo.findByEmail(autorEmail.trim().toLowerCase(Locale.ROOT)).orElse(null);
    }

    /**
     * Busca o lead mais recente do mesmo cliente (mesmo e-mail ou mesmo telefone),
     * ignorando o proprio lead passado em :exceptId quando aplicavel.
     * Retorna empty quando nao ha match.
     */
    private Optional<Lead> buscarLeadExistenteMesmoCliente(String email, String telefone, String exceptId) {
        String emailNorm = (email == null || email.isBlank())
                ? null : email.trim().toLowerCase(Locale.ROOT);
        String telDigits = normalizarTelefone(telefone);
        if (telDigits != null && telDigits.length() < 8) {
            telDigits = null;
        }
        if (emailNorm == null && telDigits == null) {
            return Optional.empty();
        }
        final String telDigitsFinal = telDigits;
        List<Lead> candidatos = leadRepo.findClienteExistente(emailNorm, telDigits).stream()
                .filter(l -> exceptId == null || !exceptId.equals(l.getId()))
                .toList();

        if (telDigitsFinal != null) {
            Optional<Lead> porTelefone = candidatos.stream()
                    .filter(l -> telDigitsFinal.equals(normalizarTelefone(l.getTelefone())))
                    .findFirst();
            if (porTelefone.isPresent()) {
                return porTelefone;
            }
        }

        return candidatos.stream().findFirst();
    }

    /**
     * Round-robin entre vendedores ativos: pega o proximo na ordem alfabetica
     * apos o vendedor do ultimo lead atribuido. Se nao houver historico, comeca
     * pelo primeiro da lista. Retorna null se nao houver vendedores cadastrados.
     */
    private Usuario proximoVendedorRoundRobin() {
        List<Usuario> vendedores = usuarioRepo.findByPerfilAndAtivoTrueOrderByNome(Perfil.VENDEDOR);
        if (vendedores.isEmpty()) {
            return null;
        }

        Optional<Lead> ultimo = leadRepo.findFirstByVendedorIsNotNullOrderByCriadoEmDesc();
        if (ultimo.isEmpty() || ultimo.get().getVendedor() == null) {
            return vendedores.get(0);
        }

        String ultimoId = ultimo.get().getVendedor().getId();
        int idx = -1;
        for (int i = 0; i < vendedores.size(); i++) {
            if (vendedores.get(i).getId().equals(ultimoId)) {
                idx = i;
                break;
            }
        }
        // Vendedor antigo nao esta mais ativo ou nao e' vendedor: comeca do inicio
        if (idx < 0) {
            return vendedores.get(0);
        }
        return vendedores.get((idx + 1) % vendedores.size());
    }

    private void aplicarRegraJornadaLisandra(Lead lead) {
        if (isLisandra(lead.getVendedor())) {
            lead.setJornada(JornadaLead.TELEVENDAS);
        }
    }

    private String normalizarTelefone(String telefone) {
        return telefone == null ? null : telefone.replaceAll("\\D", "");
    }

    private boolean isLisandra(Usuario vendedor) {
        return vendedor != null && LISANDRA_ID.equals(vendedor.getId());
    }

    private boolean isVendedorAtivo(Usuario usuario) {
        return usuario != null && usuario.isAtivo() && usuario.getPerfil() == Perfil.VENDEDOR;
    }

    private String descricaoNovaTarefa(TarefaLead tarefa) {
        if (tarefa.getVencimentoEm() == null) {
            return "Nova tarefa criada: " + tarefa.getTitulo();
        }
        return "Nova tarefa criada: " + tarefa.getTitulo() + " (vence em " + formatarData(tarefa.getVencimentoEm()) + ")";
    }

    private String badgePorTipo(String tipoEvento) {
        return switch (tipoEvento) {
            case "FOLLOW_UP" -> "Follow-up";
            case "TAREFA" -> "Tarefa";
            case "ENTRADA" -> "Entrada";
            default -> "Mudanca";
        };
    }

    private String formatarData(LocalDateTime data) {
        return data != null ? data.format(DATE_TIME) : null;
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.trim();
        return texto.isEmpty() ? null : texto;
    }

    private String vazioParaPlaceholder(String valor) {
        return valor == null || valor.isBlank() ? "Nao definido" : valor;
    }

    private String labelJornada(JornadaLead jornada) {
        return jornada != null ? jornada.getLabel() : "Nao definida";
    }

    private boolean equalsNullable(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }
}
