package com.syntra.model.enums;

public enum StatusLead {
    NOVO("Novo"),
    EM_ATENDIMENTO("Em Atendimento"),
    EM_OUTRO_ATENDIMENTO("Em Outro Atendimento"),
    AGUARDANDO_RETORNO("Aguardando Retorno"),
    CONVERTIDO("Convertido"),
    PERDIDO("Perdido"),
    CADASTRADO_NO_SITE("Cadastrado no Site");

    private final String label;

    StatusLead(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
