package com.syntra.model;

import com.syntra.model.enums.JornadaLead;
import com.syntra.model.enums.StatusLead;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lead")
public class Lead {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(length = 200)
    private String email;

    @Column(length = 30)
    private String telefone;

    @Column(length = 100)
    private String origem;

    @Column(length = 100)
    private String campanha;

    @Column(columnDefinition = "TEXT")
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private JornadaLead jornada;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private StatusLead status = StatusLead.NOVO;

    @Column(nullable = false)
    private boolean lido = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id")
    private Usuario vendedor;

    @Column(name = "dados_extras", columnDefinition = "TEXT")
    private String dadosExtras;

    @Column(name = "origem_externa", length = 50)
    private String origemExterna;

    @Column(name = "lead_externo_id", length = 100)
    private String leadExternoId;

    @Column(name = "proxima_acao", length = 255)
    private String proximaAcao;

    @Column(name = "proximo_contato_em")
    private LocalDateTime proximoContatoEm;

    @Column(name = "ultima_interacao_em")
    private LocalDateTime ultimaInteracaoEm = LocalDateTime.now();

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @OneToMany(mappedBy = "lead", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("criadoEm DESC")
    private List<ComentarioLead> comentarios = new ArrayList<>();

    @OneToMany(mappedBy = "lead", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("criadoEm DESC")
    private List<HistoricoLead> historico = new ArrayList<>();

    @OneToMany(mappedBy = "lead", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("status ASC, vencimentoEm ASC, criadoEm DESC")
    private List<TarefaLead> tarefas = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) id = java.util.UUID.randomUUID().toString();
        if (ultimaInteracaoEm == null) ultimaInteracaoEm = criadoEm;
    }

    public boolean isFollowUpAtrasado() {
        return proximoContatoEm != null
                && proximoContatoEm.isBefore(LocalDateTime.now())
                && status != StatusLead.CONVERTIDO
                && status != StatusLead.PERDIDO;
    }

    public boolean isParado() {
        if (status == StatusLead.CONVERTIDO || status == StatusLead.PERDIDO) {
            return false;
        }
        if (isFollowUpAtrasado()) {
            return true;
        }
        return ultimaInteracaoEm != null && ultimaInteracaoEm.isBefore(LocalDateTime.now().minusDays(3));
    }

    public String getId()                             { return id; }
    public void setId(String id)                      { this.id = id; }
    public String getNome()                           { return nome; }
    public void setNome(String nome)                  { this.nome = nome; }
    public String getEmail()                          { return email; }
    public void setEmail(String email)                { this.email = email; }
    public String getTelefone()                       { return telefone; }
    public void setTelefone(String telefone)          { this.telefone = telefone; }
    public String getOrigem()                         { return origem; }
    public void setOrigem(String origem)              { this.origem = origem; }
    public String getCampanha()                       { return campanha; }
    public void setCampanha(String campanha)          { this.campanha = campanha; }
    public String getMensagem()                       { return mensagem; }
    public void setMensagem(String mensagem)          { this.mensagem = mensagem; }
    public JornadaLead getJornada()                   { return jornada; }
    public void setJornada(JornadaLead jornada)       { this.jornada = jornada; }
    public StatusLead getStatus()                     { return status; }
    public void setStatus(StatusLead status)          { this.status = status; }
    public boolean isLido()                           { return lido; }
    public void setLido(boolean lido)                 { this.lido = lido; }
    public Usuario getVendedor()                      { return vendedor; }
    public void setVendedor(Usuario vendedor)         { this.vendedor = vendedor; }
    public String getDadosExtras()                    { return dadosExtras; }
    public void setDadosExtras(String dadosExtras)    { this.dadosExtras = dadosExtras; }

    public boolean isWhatsappContact() {
        return telefone != null && !getTelefoneSomenteDigitos().isBlank();
    }

    public String getTelefoneSomenteDigitos() {
        if (telefone == null) {
            return "";
        }
        return telefone.replaceAll("\\D", "");
    }

    public String getWhatsappUrl() {
        String numero = getTelefoneSomenteDigitos();
        if (numero.isBlank()) {
            return null;
        }
        if (numero.startsWith("0")) {
            numero = numero.substring(1);
        }
        String mensagem = URLEncoder.encode(
                        "Olá! Tudo bem?\n\n"
                                + "Aqui é da Pró Colchões, representante das marcas Probel e Prodormir. "
                                + "Recebemos o seu cadastro através da nossa página B2B e gostaríamos "
                                + "de entender melhor a sua necessidade.\n\n"
                                + "Trabalhamos com condições especiais para lojistas, hotéis e distribuidores. "
                                + "Antes de seguirmos, queria confirmar com você: já está sendo atendido por "
                                + "algum de nossos vendedores ou posso dar continuidade no seu atendimento por aqui?",
                        StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "https://wa.me/55" + numero + "?text=" + mensagem;
    }

    public String getOrigemExterna()                  { return origemExterna; }
    public void setOrigemExterna(String origemExterna){ this.origemExterna = origemExterna; }
    public String getLeadExternoId()                  { return leadExternoId; }
    public void setLeadExternoId(String leadExternoId){ this.leadExternoId = leadExternoId; }
    public String getProximaAcao()                    { return proximaAcao; }
    public void setProximaAcao(String proximaAcao)    { this.proximaAcao = proximaAcao; }
    public LocalDateTime getProximoContatoEm()        { return proximoContatoEm; }
    public void setProximoContatoEm(LocalDateTime v)  { this.proximoContatoEm = v; }
    public LocalDateTime getUltimaInteracaoEm()       { return ultimaInteracaoEm; }
    public void setUltimaInteracaoEm(LocalDateTime v) { this.ultimaInteracaoEm = v; }
    public LocalDateTime getCriadoEm()                { return criadoEm; }
    public void setCriadoEm(LocalDateTime v)          { this.criadoEm = v; }
    public LocalDateTime getAtualizadoEm()            { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime v)      { this.atualizadoEm = v; }
    public List<ComentarioLead> getComentarios()      { return comentarios; }
    public List<HistoricoLead> getHistorico()         { return historico; }
    public List<TarefaLead> getTarefas()              { return tarefas; }
}
