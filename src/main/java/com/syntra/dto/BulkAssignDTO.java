package com.syntra.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class BulkAssignDTO {

    @NotEmpty(message = "Selecione ao menos um lead.")
    private List<@Size(max = 36, message = "Identificador de lead inválido.") String> leadIds;

    @Size(max = 36, message = "Identificador de vendedor inválido.")
    private String vendedorId;

    public List<String> getLeadIds() {
        return leadIds;
    }

    public void setLeadIds(List<String> leadIds) {
        this.leadIds = leadIds;
    }

    public String getVendedorId() {
        return vendedorId;
    }

    public void setVendedorId(String vendedorId) {
        this.vendedorId = vendedorId;
    }
}
