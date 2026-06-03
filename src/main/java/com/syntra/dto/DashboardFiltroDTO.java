package com.syntra.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class DashboardFiltroDTO {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFim;

    public LocalDate getDataInicio()        { return dataInicio; }
    public void setDataInicio(LocalDate d)  { this.dataInicio = d; }
    public LocalDate getDataFim()           { return dataFim; }
    public void setDataFim(LocalDate d)     { this.dataFim = d; }
}
