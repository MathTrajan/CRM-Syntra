package com.syntra.model.enums;

public enum StatusLead {
    NOVO("Novo"),
    EM_ATENDIMENTO("Em Atendimento"),
    AGUARDANDO_RETORNO("Aguardando Retorno"),
    CONVERTIDO("Convertido"),
    PERDIDO("Perdido");

    private final String label;

    StatusLead(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
