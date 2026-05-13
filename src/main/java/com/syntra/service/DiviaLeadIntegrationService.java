package com.syntra.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syntra.dto.DiviaSyncResultDTO;
import com.syntra.dto.LeadFiltroDTO;
import com.syntra.dto.divia.DiviaLeadDTO;
import com.syntra.dto.divia.DiviaLeadResponseDTO;
import com.syntra.model.Lead;
import com.syntra.repository.LeadRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
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

    private static final String ORIGEM_EXTERNA_DIVIA = "DIVIA";

    private final LeadRepository leadRepository;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiToken;

    public DiviaLeadIntegrationService(LeadRepository leadRepository,
                                       ObjectMapper objectMapper,
                                       RestClient.Builder restClientBuilder,
                                       @Value("${syntra.integrations.divia.base-url}") String baseUrl,
                                       @Value("${syntra.integrations.divia.token:}") String apiToken) {
        this.leadRepository = leadRepository;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.apiToken = apiToken;
    }

    @Transactional
    public DiviaSyncResultDTO sincronizar(LeadFiltroDTO filtro) {
        validarConfiguracao();

        DiviaSyncResultDTO resultado = new DiviaSyncResultDTO();
        int pagina = 1;
        int ultimaPagina = 1;

        do {
            DiviaLeadResponseDTO resposta = buscarPagina(filtro, pagina);
            List<DiviaLeadDTO> leads = resposta.getData() != null ? resposta.getData() : List.of();

            for (DiviaLeadDTO dto : leads) {
                boolean novo = importarOuAtualizar(dto);
                if (novo) {
                    resultado.setImportados(resultado.getImportados() + 1);
                } else {
                    resultado.setAtualizados(resultado.getAtualizados() + 1);
                }
                resultado.setRecebidos(resultado.getRecebidos() + 1);
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

    private boolean importarOuAtualizar(DiviaLeadDTO dto) {
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
}
