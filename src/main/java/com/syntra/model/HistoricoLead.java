package com.syntra.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historico_lead")
public class HistoricoLead {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    @Column(nullable = false, length = 80)
    private String campo;

    @Column(name = "tipo_evento", nullable = false, length = 30)
    private String tipoEvento = "ALTERACAO";

    @Column(name = "origem_acao", nullable = false, length = 30)
    private String origemAcao = "PAINEL";

    @Column(name = "valor_antes", columnDefinition = "TEXT")
    private String valorAntes;

    @Column(name = "valor_depois", columnDefinition = "TEXT")
    private String valorDepois;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) id = java.util.UUID.randomUUID().toString();
    }

    public String getId()                      { return id; }
    public void setId(String id)               { this.id = id; }
    public Lead getLead()                      { return lead; }
    public void setLead(Lead lead)             { this.lead = lead; }
    public Usuario getAutor()                  { return autor; }
    public void setAutor(Usuario autor)        { this.autor = autor; }
    public String getCampo()                   { return campo; }
    public void setCampo(String campo)         { this.campo = campo; }
    public String getTipoEvento()              { return tipoEvento; }
    public void setTipoEvento(String tipoEvento) { this.tipoEvento = tipoEvento; }
    public String getOrigemAcao()              { return origemAcao; }
    public void setOrigemAcao(String origemAcao) { this.origemAcao = origemAcao; }
    public String getValorAntes()              { return valorAntes; }
    public void setValorAntes(String v)        { this.valorAntes = v; }
    public String getValorDepois()             { return valorDepois; }
    public void setValorDepois(String v)       { this.valorDepois = v; }
    public String getDescricao()               { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public LocalDateTime getCriadoEm()         { return criadoEm; }
    public void setCriadoEm(LocalDateTime v)   { this.criadoEm = v; }
}
