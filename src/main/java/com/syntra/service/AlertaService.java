package com.syntra.service;

import com.syntra.dto.AlertaLead;
import com.syntra.model.Lead;
import com.syntra.model.enums.SeveridadeAlerta;
import com.syntra.model.enums.StatusLead;
import com.syntra.repository.LeadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço que avalia alertas de atenção por vendedor.
 *
 * Regra geral: um lead "parado" é aquele que ficou sem interação
 * por mais dias do que o permitido para o seu status atual.
 * A referência de tempo é ultimaInteracaoEm, com fallback para criadoEm.
 *
 * Os alertas são ESTRITAMENTE por vendedor: só leads onde
 * lead.vendedor.id == vendedorId entram no cálculo.
 * Leads sem vendedor não geram alertas.
 */
@Service
public class AlertaService {

    // ── Limiares NOVO ──────────────────────────────────────────────
    private static final long NOVO_URGENTE   = 2;
    private static final long NOVO_ATENCAO   = 1;

    // ── Limiares CADASTRADO_NO_SITE ────────────────────────────────
    private static final long SITE_URGENTE   = 6;
    private static final long SITE_ATENCAO   = 3;

    // ── Limiares EM_ATENDIMENTO ─────────────────────────────────────
    private static final long ATEND_URGENTE  = 7;
    private static final long ATEND_ATENCAO  = 4;

    // ── Limiares AGUARDANDO_RETORNO ────────────────────────────────
    private static final long AGUARD_URGENTE = 14;
    private static final long AGUARD_ATENCAO = 7;

    private final LeadRepository leadRepository;

    public AlertaService(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    /**
     * Avalia um único lead em relação ao momento {@code agora} e retorna um
     * AlertaLead quando as regras de negócio forem satisfeitas, ou {@code null}
     * quando não houver motivo de alerta.
     *
     * Método estático e puro — sem estado, sem I/O — para facilitar testes unitários.
     *
     * @param lead  lead a avaliar (não deve ser null)
     * @param agora instante de referência para o cálculo de dias
     * @return AlertaLead preenchido, ou null se não há alerta
     */
    public static AlertaLead avaliar(Lead lead, LocalDateTime agora) {
        // Calcula a base de tempo: prefere ultimaInteracaoEm, usa criadoEm como fallback
        LocalDateTime base = lead.getUltimaInteracaoEm() != null
                ? lead.getUltimaInteracaoEm()
                : lead.getCriadoEm();

        long d = ChronoUnit.DAYS.between(base, agora);

        StatusLead status = lead.getStatus();

        // Determina severidade e mensagem conforme o status — URGENTE tem prioridade
        SeveridadeAlerta severidade;
        String mensagem;

        switch (status) {
            case NOVO -> {
                if (d >= NOVO_URGENTE) {
                    severidade = SeveridadeAlerta.URGENTE;
                    mensagem   = "Lead novo esfriando: " + d + " dias e ninguém atendeu";
                } else if (d >= NOVO_ATENCAO) {
                    severidade = SeveridadeAlerta.ATENCAO;
                    mensagem   = "Lead novo sem atendimento há " + d + " dia(s) — assuma o contato";
                } else {
                    return null; // abaixo do limiar mínimo
                }
            }
            case CADASTRADO_NO_SITE -> {
                if (d >= SITE_URGENTE) {
                    severidade = SeveridadeAlerta.URGENTE;
                    mensagem   = "Cadastro do site parado há " + d
                                 + " dias — risco de abandono, retome o contato";
                } else if (d >= SITE_ATENCAO) {
                    severidade = SeveridadeAlerta.ATENCAO;
                    mensagem   = "Cadastro no site feito há " + d
                                 + " dias — dê sequência (confirme acesso/retorno do cliente)";
                } else {
                    return null;
                }
            }
            case EM_ATENDIMENTO -> {
                if (d >= ATEND_URGENTE) {
                    severidade = SeveridadeAlerta.URGENTE;
                    mensagem   = "Atendimento travado há " + d
                                 + " dias — risco de perder; priorize ou reavalie";
                } else if (d >= ATEND_ATENCAO) {
                    severidade = SeveridadeAlerta.ATENCAO;
                    mensagem   = "Atendimento sem avanço há " + d
                                 + " dias — defina o próximo passo";
                } else {
                    return null;
                }
            }
            case AGUARDANDO_RETORNO -> {
                if (d >= AGUARD_URGENTE) {
                    severidade = SeveridadeAlerta.URGENTE;
                    mensagem   = d + " dias sem retorno — converter ou marcar como perdido";
                } else if (d >= AGUARD_ATENCAO) {
                    severidade = SeveridadeAlerta.ATENCAO;
                    mensagem   = "Cliente não retorna há " + d + " dias — faça um follow-up";
                } else {
                    return null;
                }
            }
            // EM_OUTRO_ATENDIMENTO, CONVERTIDO, PERDIDO e qualquer outro — sem alerta
            default -> {
                return null;
            }
        }

        return new AlertaLead(
                lead.getId(),
                lead.getNome(),
                lead.getStatus().getLabel(),
                severidade,
                d,
                mensagem);
    }

    /**
     * Retorna a lista de alertas ativos para o vendedor identificado por {@code vendedorId},
     * usando o instante {@code agora} como referência.
     *
     * Apenas leads com status candidatos (NOVO, CADASTRADO_NO_SITE, EM_ATENDIMENTO,
     * AGUARDANDO_RETORNO) e vinculados ao vendedor são consultados.
     *
     * A lista resultante está ordenada: URGENTE antes de ATENCAO;
     * dentro de cada severidade, mais dias parado primeiro.
     *
     * @param vendedorId ID do Usuario/vendedor logado
     * @param agora      instante de referência
     * @return lista de alertas, nunca null (pode ser vazia)
     */
    @Transactional(readOnly = true)
    public List<AlertaLead> alertasDoVendedor(String vendedorId, LocalDateTime agora) {
        // Busca apenas os statuses candidatos a gerar alertas
        List<StatusLead> candidatos = List.of(
                StatusLead.NOVO,
                StatusLead.CADASTRADO_NO_SITE,
                StatusLead.EM_ATENDIMENTO,
                StatusLead.AGUARDANDO_RETORNO);

        return leadRepository
                .findByVendedorIdAndStatusInOrderByUltimaInteracaoEmAsc(vendedorId, candidatos)
                .stream()
                .map(lead -> avaliar(lead, agora))
                .filter(alerta -> alerta != null)
                .sorted(
                    // URGENTE primeiro
                    Comparator.<AlertaLead, Integer>comparing(
                            a -> a.getSeveridade() == SeveridadeAlerta.URGENTE ? 0 : 1)
                    // dentro da mesma severidade, mais dias parado primeiro
                    .thenComparingLong(a -> -a.getDiasParado()))
                .collect(Collectors.toList());
    }

    /**
     * Sobrecarga conveniente que usa LocalDateTime.now() como referência.
     *
     * @param vendedorId ID do vendedor logado
     * @return lista de alertas ativos
     */
    public List<AlertaLead> alertasDoVendedor(String vendedorId) {
        return alertasDoVendedor(vendedorId, LocalDateTime.now());
    }
}
