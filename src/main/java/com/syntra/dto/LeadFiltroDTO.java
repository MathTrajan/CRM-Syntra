package com.syntra.dto;

import com.syntra.model.enums.StatusLead;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class LeadFiltroDTO {

    private StatusLead status;
    private String vendedorId;
    private String busca;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFim;

    private int page = 0;
    private int size = 50;

    public StatusLead getStatus()                 { return status; }
    public void setStatus(StatusLead status)      { this.status = status; }
    public String getVendedorId()                 { return vendedorId; }
    public void setVendedorId(String v)           { this.vendedorId = v; }
    public String getBusca()                      { return busca; }
    public void setBusca(String busca)            { this.busca = busca; }
    public LocalDate getDataInicio()              { return dataInicio; }
    public void setDataInicio(LocalDate d)        { this.dataInicio = d; }
    public LocalDate getDataFim()                 { return dataFim; }
    public void setDataFim(LocalDate d)           { this.dataFim = d; }
    public int getPage()                          { return page; }
    public void setPage(int page)                 { this.page = Math.max(page, 0); }
    public int getSize()                          { return size; }
    public void setSize(int size)                 { this.size = Math.min(Math.max(size, 1), 200); }
}
