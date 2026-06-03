package com.syntra.dto;

import com.syntra.model.enums.JornadaLead;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CriarLeadManualDTO {

    @NotBlank(message = "Nome é obrigatório.")
    @Size(max = 200)
    private String nome;

    @Email(message = "Informe um e-mail válido.")
    @Size(max = 200)
    private String email;

    @Size(max = 30)
    private String telefone;

    @Size(max = 100)
    private String origem;

    @Size(max = 100)
    private String campanha;

    private String mensagem;

    private JornadaLead jornada;

    private String vendedorId;

    public String getNome()                       { return nome; }
    public void setNome(String nome)              { this.nome = nome; }
    public String getEmail()                      { return email; }
    public void setEmail(String email)            { this.email = email; }
    public String getTelefone()                   { return telefone; }
    public void setTelefone(String telefone)      { this.telefone = telefone; }
    public String getOrigem()                     { return origem; }
    public void setOrigem(String origem)          { this.origem = origem; }
    public String getCampanha()                   { return campanha; }
    public void setCampanha(String campanha)      { this.campanha = campanha; }
    public String getMensagem()                   { return mensagem; }
    public void setMensagem(String mensagem)      { this.mensagem = mensagem; }
    public JornadaLead getJornada()               { return jornada; }
    public void setJornada(JornadaLead jornada)   { this.jornada = jornada; }
    public String getVendedorId()                 { return vendedorId; }
    public void setVendedorId(String vendedorId)  { this.vendedorId = vendedorId; }
}
