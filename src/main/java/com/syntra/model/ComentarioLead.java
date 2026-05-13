package com.syntra.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comentario_lead")
public class ComentarioLead {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) id = java.util.UUID.randomUUID().toString();
    }

    public String getId()                    { return id; }
    public void setId(String id)             { this.id = id; }
    public Lead getLead()                    { return lead; }
    public void setLead(Lead lead)           { this.lead = lead; }
    public Usuario getAutor()                { return autor; }
    public void setAutor(Usuario autor)      { this.autor = autor; }
    public String getTexto()                 { return texto; }
    public void setTexto(String texto)       { this.texto = texto; }
    public LocalDateTime getCriadoEm()       { return criadoEm; }
    public void setCriadoEm(LocalDateTime v) { this.criadoEm = v; }
}
