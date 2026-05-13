package com.syntra.model.enums;

public enum StatusTarefaLead {
    PENDENTE("Pendente"),
    CONCLUIDA("Concluída");

    private final String label;

    StatusTarefaLead(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
