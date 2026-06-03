package com.syntra.model.enums;

public enum JornadaLead {
    TELEVENDAS("Televendas"),
    OMNI("Omni"),
    LOJA_FISICA("Loja Física"),
    VENDA_DIRETA("Venda Direta");

    private final String label;

    JornadaLead(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
