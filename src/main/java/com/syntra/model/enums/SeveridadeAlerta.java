package com.syntra.model.enums;

/**
 * Severidade de um alerta de atenção para o vendedor.
 * ATENCAO: prazo ultrapassado mas ainda recuperável.
 * URGENTE: situação crítica que exige ação imediata.
 */
public enum SeveridadeAlerta {

    ATENCAO("Atenção"),
    URGENTE("Urgente");

    private final String label;

    SeveridadeAlerta(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
