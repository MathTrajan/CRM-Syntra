package com.syntra.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syntra.dto.DiviaSyncResultDTO;
import com.syntra.dto.LeadFiltroDTO;
import com.syntra.dto.divia.DiviaLeadDTO;
import com.syntra.dto.divia.DiviaLeadResponseDTO;
import com.syntra.model.Lead;
import com.syntra.model.Usuario;
import com.syntra.model.enums.Perfil;
import com.syntra.repository.LeadRepository;
import com.syntra.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DiviaLeadIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(DiviaLeadIntegrationService.class);
    private static final String ORIGEM_EXTERNA_DIVIA = "DIVIA";
    private static final String LISANDRA_ID = "0cad46fb-5795-4683-9135-18b051e6b32b";

    private final LeadRepository leadRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiToken;
    /** Auto-injecao da propria classe para que chamadas a importarOuAtualizar passem
     *  pelo proxy Spring e respeitem o @Transactional(REQUIRES_NEW) - chamada direta
     *  em metodo do mesmo bean bypassa o proxy e cairia na transacao do chamador. */
    private final org.springframework.context.ApplicationContext appContext;

    public DiviaLeadIntegrationService(LeadRepository leadRepository,
                                       UsuarioRepository usuarioRepository,
                                       ObjectMapper objectMapper,
                                       RestClient.Builder restClientBuilder,
                                       org.springframework.context.ApplicationContext appContext,
                                       @Value("${syntra.integrations.divia.base-url}") String baseUrl,
                                       @Value("${syntra.integrations.divia.token:}") String apiToken) {
        this.leadRepository = leadRepository;
        this.usuarioRepository = usuarioRepository;
        this.objectMapper = objectMapper;
        this.appContext = appContext;
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.apiToken = apiToken;
    }

    /**
     * Sincroniza com a fonte externa de leads paginando ate o fim.
     *
     * IMPORTANTE: este metodo NAO e' @Transactional - cada lead e' salvo em sua
     * propria transacao via {@link #importarOuAtualizar} (REQUIRES_NEW). Assim,
     * se a API cair na pagina 5 de 10, as paginas 1-4 ja estarao persistidas e
     * nao sofrem rollback. O usuario ve qual foi o ultimo ponto seguro pelo
     * contador no DiviaSyncResultDTO retornado e pelo log.
     */
    public DiviaSyncResultDTO sincronizar(LeadFiltroDTO filtro) {
        validarConfiguracao();

        // O bean proxy permite que cada chamada respeite o @Transactional propio.
        DiviaLeadIntegrationService self = appContext.getBean(DiviaLeadIntegrationService.class);

        DiviaSyncResultDTO resultado = new DiviaSyncResultDTO();
        int pagina = 1;
        int ultimaPagina = 1;

        do {
            DiviaLeadResponseDTO resposta;
            try {
                resposta = buscarPagina(filtro, pagina);
            } catch (RuntimeException apiFalha) {
                // Falha de API: preserva o que ja foi salvo nas paginas anteriores
                // e propaga a exception com o estado parcial nos contadores.
                log.warn("Sync Divia interrompido na pagina {}: {}. Persistidos ate aqui: {} novo(s), {} atualizado(s).",
                        pagina, apiFalha.getMessage(),
                        resultado.getImportados(), resultado.getAtualizados());
                throw apiFalha;
            }

            List<DiviaLeadDTO> leads = resposta.getData() != null ? resposta.getData() : List.of();

            for (DiviaLeadDTO dto : leads) {
                try {
                    boolean novo = self.importarOuAtualizar(dto);
                    if (novo) {
                        resultado.setImportados(resultado.getImportados() + 1);
                    } else {
                        resultado.setAtualizados(resultado.getAtualizados() + 1);
                    }
                    resultado.setRecebidos(resultado.getRecebidos() + 1);
                } catch (RuntimeException leadFalha) {
                    // Um lead com defeito (campo invalido, etc.) nao deve abortar
                    // o restante do batch. Loga e segue para o proximo.
                    log.error("Falha ao persistir lead {} da Divia: {}",
                            dto.getId(), leadFalha.getMessage());
                }
            }

            if (resposta.getMeta() != null && resposta.getMeta().getLastPage() > 0) {
                ultimaPagina = resposta.getMeta().getLastPage();
            }
            pagina++;
        } while (pagina <= ultimaPagina);

        return resultado;
    }

    private DiviaLeadResponseDTO buscarPagina(LeadFiltroDTO filtro, int pagina) {
        DiviaLeadResponseDTO resposta = restClient.get()
                .uri(uriBuilder -> montarUri(uriBuilder, filtro, pagina))
                .header("Authorization", "Bearer " + apiToken)
                .retrieve()
                .body(DiviaLeadResponseDTO.class);

        if (resposta == null) {
            throw new IllegalStateException("A API da Divia retornou uma resposta vazia.");
        }
        return resposta;
    }

    private java.net.URI montarUri(UriBuilder builder, LeadFiltroDTO filtro, int pagina) {
        UriBuilder uriBuilder = builder.path("/leads")
                .queryParam("page", pagina)
                .queryParam("per_page", filtro.getSize())
                .queryParam("order", "desc");

        if (filtro.getBusca() != null && !filtro.getBusca().isBlank()) {
            uriBuilder.queryParam("search", filtro.getBusca().trim());
        }
        if (filtro.getDataInicio() != null) {
            uriBuilder.queryParam("created_from", filtro.getDataInicio());
        }
        if (filtro.getDataFim() != null) {
            uriBuilder.queryParam("created_to", filtro.getDataFim());
        }
        return uriBuilder.build();
    }

    /**
     * Salva um unico lead em sua propria transacao. REQUIRES_NEW garante que
     * a operacao commit-e independentemente do chamador - se a sincronizacao
     * abortar depois, este lead ja esta gravado.
     *
     * E' public por necessidade do proxy Spring (chamadas internas a metodos
     * private nao passam pelo proxy e perdem o @Transactional).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean importarOuAtualizar(DiviaLeadDTO dto) {
        if (dto.getId() == null || dto.getId().isBlank()) {
            throw new IllegalStateException("A API da Divia retornou um lead sem identificador.");
        }

        Lead lead = leadRepository.findByOrigemExternaAndLeadExternoId(ORIGEM_EXTERNA_DIVIA, dto.getId()).orElse(null);
        boolean novo = lead == null;

        if (novo) {
            lead = new Lead();
            lead.setOrigemExterna(ORIGEM_EXTERNA_DIVIA);
            lead.setLeadExternoId(dto.getId());
            lead.setLido(false);

            // Antes de cair no round-robin, tenta identificar um cliente recorrente
            // pelo e-mail/telefone para herdar vendedor + jornada + status do atendimento anterior.
            Lead existente = buscarLeadExistenteMesmoCliente(limpar(dto.getEmail()), limpar(dto.getTelefone()));
            if (existente != null) {
                if (existente.getVendedor() != null) {
                    lead.setVendedor(existente.getVendedor());
                }
                if (existente.getJornada() != null) {
                    lead.setJornada(existente.getJornada());
                }
                if (existente.getStatus() != null) {
                    lead.setStatus(existente.getStatus());
                }
            } else {
                Usuario vendedorAuto = proximoVendedorRoundRobin();
                if (vendedorAuto != null) {
                    lead.setVendedor(vendedorAuto);
                }
            }
            aplicarRegraJornadaLisandra(lead);
        }

        lead.setNome(preencherNome(dto));
        lead.setEmail(limpar(dto.getEmail()));
        lead.setTelefone(limpar(dto.getTelefone()));
        lead.setOrigem(definirOrigem(dto));
        lead.setCampanha(definirCampanha(dto));
        lead.setMensagem(limpar(dto.getMensagem()));
        lead.setDadosExtras(serializarDadosExtras(dto));

        LocalDateTime criadoEm = converterData(dto.getData());
        if (novo && criadoEm != null) {
            lead.setCriadoEm(criadoEm);
            lead.setUltimaInteracaoEm(criadoEm);
        }

        leadRepository.save(lead);
        return novo;
    }

    /** Encontra o lead mais recente do mesmo cliente (email ou telefone). */
    private Lead buscarLeadExistenteMesmoCliente(String email, String telefone) {
        String emailNorm = (email == null || email.isBlank())
                ? null : email.trim().toLowerCase();
        String telDigits = (telefone == null) ? null : telefone.replaceAll("\\D", "");
        if (telDigits != null && telDigits.length() < 8) {
            telDigits = null;
        }
        if (emailNorm == null && telDigits == null) {
            return null;
        }
        List<Lead> matches = leadRepository.findClienteExistente(emailNorm, telDigits);
        return matches.isEmpty() ? null : matches.get(0);
    }

    /** Mesma logica de LeadService.proximoVendedorRoundRobin - duplicada
     *  para evitar dependencia ciclica entre os dois services. */
    private Usuario proximoVendedorRoundRobin() {
        List<Usuario> vendedores = usuarioRepository.findByPerfilAndAtivoTrueOrderByNome(Perfil.VENDEDOR);
        if (vendedores.isEmpty()) {
            return null;
        }
        return leadRepository.findFirstByVendedorIsNotNullOrderByCriadoEmDesc()
                .map(Lead::getVendedor)
                .map(v -> {
                    for (int i = 0; i < vendedores.size(); i++) {
                        if (vendedores.get(i).getId().equals(v.getId())) {
                            return vendedores.get((i + 1) % vendedores.size());
                        }
                    }
                    return vendedores.get(0);
                })
                .orElse(vendedores.get(0));
    }

    private void aplicarRegraJornadaLisandra(Lead lead) {
        if (isLisandra(lead.getVendedor())) {
            lead.setJornada(com.syntra.model.enums.JornadaLead.TELEVENDAS);
        }
    }

    private boolean isLisandra(Usuario vendedor) {
        return vendedor != null && LISANDRA_ID.equals(vendedor.getId());
    }

    private String preencherNome(DiviaLeadDTO dto) {
        String nome = limpar(dto.getNome());
        return nome != null ? nome : "Lead Divia " + dto.getId();
    }

    private String definirOrigem(DiviaLeadDTO dto) {
        if (limpar(dto.getOrigem()) != null) {
            return limpar(dto.getOrigem());
        }
        if (limpar(dto.getTipo()) != null) {
            return limpar(dto.getTipo());
        }
        return ORIGEM_EXTERNA_DIVIA;
    }

    private String definirCampanha(DiviaLeadDTO dto) {
        if (limpar(dto.getAnuncio()) != null) {
            return limpar(dto.getAnuncio());
        }
        if (limpar(dto.getPalavraChave()) != null) {
            return limpar(dto.getPalavraChave());
        }
        return limpar(dto.getInteresse());
    }

    private String serializarDadosExtras(DiviaLeadDTO dto) {
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("qualificacao", limpar(dto.getQualificacao()));
        extras.put("tipo", limpar(dto.getTipo()));
        extras.put("referrer", limpar(dto.getReferrer()));
        extras.put("palavraChave", limpar(dto.getPalavraChave()));
        extras.put("anuncio", limpar(dto.getAnuncio()));
        extras.put("interesse", limpar(dto.getInteresse()));
        extras.put("dadosAdicionais", dto.getDadosAdicionais());
        extras.put("origemExterna", ORIGEM_EXTERNA_DIVIA);
        extras.put("leadExternoId", dto.getId());
        extras.values().removeIf(value -> value == null || (value instanceof Map<?, ?> map && map.isEmpty()));

        try {
            return objectMapper.writeValueAsString(extras);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Nao foi possivel serializar os dados extras do lead da Divia.", ex);
        }
    }

    private LocalDateTime converterData(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(data).toLocalDateTime();
        } catch (DateTimeParseException ex) {
            throw new IllegalStateException("A API da Divia retornou uma data invalida: " + data, ex);
        }
    }

    private String limpar(String valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.trim();
        return texto.isEmpty() ? null : texto;
    }

    private void validarConfiguracao() {
        if (apiToken == null || apiToken.isBlank()) {
            throw new IllegalStateException("Configure a variavel DIVIA_API_TOKEN para sincronizar os leads da Divia.");
        }
    }

    /**
     * Sincronizacao automatica periodica. Roda a cada 30 minutos. Garante que novos
     * leads criados na fonte externa entre os cliques manuais nunca sejam perdidos.
     * Skippa silenciosamente quando a integracao nao esta configurada (dev local).
     */
    @Scheduled(cron = "0 */30 * * * *")
    public void sincronizarAutomatico() {
        if (apiToken == null || apiToken.isBlank()) {
            log.debug("Sync automatico ignorado: DIVIA_API_TOKEN nao configurado.");
            return;
        }
        try {
            DiviaSyncResultDTO resultado = sincronizar(new LeadFiltroDTO());
            log.info("Sync automatico Divia OK: {} novo(s), {} atualizado(s), {} recebido(s).",
                    resultado.getImportados(), resultado.getAtualizados(), resultado.getRecebidos());
        } catch (RuntimeException ex) {
            log.warn("Sync automatico Divia falhou: {}", ex.getMessage());
        }
    }
}
