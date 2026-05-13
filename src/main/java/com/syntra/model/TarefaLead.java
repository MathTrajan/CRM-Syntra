package com.syntra.model;

import com.syntra.model.enums.StatusTarefaLead;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "tarefa_lead")
public class TarefaLead {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "vencimento_em")
    private LocalDateTime vencimentoEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusTarefaLead status = StatusTarefaLead.PENDENTE;

    @Column(name = "concluida_em")
    private LocalDateTime concluidaEm;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) id = java.util.UUID.randomUUID().toString();
    }

    public boolean isAtrasada() {
        return status == StatusTarefaLead.PENDENTE && vencimentoEm != null && vencimentoEm.isBefore(LocalDateTime.now());
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Lead getLead() { return lead; }
    public void setLead(Lead lead) { this.lead = lead; }
    public Usuario getAutor() { return autor; }
    public void setAutor(Usuario autor) { this.autor = autor; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public LocalDateTime getVencimentoEm() { return vencimentoEm; }
    public void setVencimentoEm(LocalDateTime vencimentoEm) { this.vencimentoEm = vencimentoEm; }
    public StatusTarefaLead getStatus() { return status; }
    public void setStatus(StatusTarefaLead status) { this.status = status; }
    public LocalDateTime getConcluidaEm() { return concluidaEm; }
    public void setConcluidaEm(LocalDateTime concluidaEm) { this.concluidaEm = concluidaEm; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
