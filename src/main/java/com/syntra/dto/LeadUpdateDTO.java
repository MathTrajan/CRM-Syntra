package com.syntra.dto;

import com.syntra.model.enums.StatusLead;
import jakarta.validation.constraints.Size;

public class LeadUpdateDTO {

    private StatusLead status;
    @Size(max = 36, message = "Identificador de vendedor inválido.")
    private String vendedorId;
    private boolean lido;

    public StatusLead getStatus()            { return status; }
    public void setStatus(StatusLead status) { this.status = status; }
    public String getVendedorId()            { return vendedorId; }
    public void setVendedorId(String v)      { this.vendedorId = v; }
    public boolean isLido()                  { return lido; }
    public void setLido(boolean lido)        { this.lido = lido; }
}
