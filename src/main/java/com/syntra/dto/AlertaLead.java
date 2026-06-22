package com.syntra.dto;

import com.syntra.model.enums.SeveridadeAlerta;

/**
 * DTO imutável que representa um alerta de atenção sobre um lead parado.
 * Criado pelo AlertaService e exibido na Central de Atenção do dashboard.
 */
public class AlertaLead {

    /** ID do lead alvo do alerta. */
    private final String leadId;

    /** Nome do lead para exibição. */
    private final String leadNome;

    /** Label legível do status atual do lead (ex.: "Em Atendimento"). */
    private final String statusLabel;

    /** Gravidade do alerta. */
    private final SeveridadeAlerta severidade;

    /** Número de dias sem interação calculado no momento da avaliação. */
    private final long diasParado;

    /** Mensagem orientativa para o vendedor, já com o número de dias interpolado. */
    private final String mensagem;

    public AlertaLead(String leadId,
                      String leadNome,
                      String statusLabel,
                      SeveridadeAlerta severidade,
                      long diasParado,
                      String mensagem) {
        this.leadId     = leadId;
        this.leadNome   = leadNome;
        this.statusLabel = statusLabel;
        this.severidade  = severidade;
        this.diasParado  = diasParado;
        this.mensagem    = mensagem;
    }

    public String getLeadId()             { return leadId; }
    public String getLeadNome()           { return leadNome; }
    public String getStatusLabel()        { return statusLabel; }
    public SeveridadeAlerta getSeveridade() { return severidade; }
    public long getDiasParado()           { return diasParado; }
    public String getMensagem()           { return mensagem; }
}
