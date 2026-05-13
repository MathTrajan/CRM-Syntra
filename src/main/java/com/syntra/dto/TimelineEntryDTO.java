package com.syntra.dto;

import java.time.LocalDateTime;

public class TimelineEntryDTO {

    private String tipo;
    private String titulo;
    private String descricao;
    private String autor;
    private LocalDateTime data;
    private String origem;
    private String valorAntes;
    private String valorDepois;
    private String badge;

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }
    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime data) { this.data = data; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    public String getValorAntes() { return valorAntes; }
    public void setValorAntes(String valorAntes) { this.valorAntes = valorAntes; }
    public String getValorDepois() { return valorDepois; }
    public void setValorDepois(String valorDepois) { this.valorDepois = valorDepois; }
    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }
}
