package com.syntra.model;

import com.syntra.model.enums.Perfil;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Perfil perfil = Perfil.VENDEDOR;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "receber_lembretes", nullable = false)
    private boolean receberLembretes = true;

    @Column(name = "resumo_diario", nullable = false)
    private boolean resumoDiario = false;

    @Column(name = "timeline_compacta", nullable = false)
    private boolean timelineCompacta = false;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) id = java.util.UUID.randomUUID().toString();
    }

    public String getId()                       { return id; }
    public void setId(String id)                { this.id = id; }
    public String getNome()                     { return nome; }
    public void setNome(String nome)            { this.nome = nome; }
    public String getEmail()                    { return email; }
    public void setEmail(String email)          { this.email = email; }
    public String getSenha()                    { return senha; }
    public void setSenha(String senha)          { this.senha = senha; }
    public Perfil getPerfil()                   { return perfil; }
    public void setPerfil(Perfil perfil)        { this.perfil = perfil; }
    public boolean isAtivo()                    { return ativo; }
    public void setAtivo(boolean ativo)         { this.ativo = ativo; }
    public boolean isReceberLembretes()         { return receberLembretes; }
    public void setReceberLembretes(boolean v)  { this.receberLembretes = v; }
    public boolean isResumoDiario()             { return resumoDiario; }
    public void setResumoDiario(boolean v)      { this.resumoDiario = v; }
    public boolean isTimelineCompacta()         { return timelineCompacta; }
    public void setTimelineCompacta(boolean v)  { this.timelineCompacta = v; }
    public LocalDateTime getCriadoEm()          { return criadoEm; }
    public void setCriadoEm(LocalDateTime v)    { this.criadoEm = v; }
    public LocalDateTime getAtualizadoEm()      { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime v){ this.atualizadoEm = v; }
}
