package com.syntra.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashMap;
import java.util.Map;

public class WebhookPayloadDTO {

    @NotBlank(message = "Campo nome é obrigatório")
    @Size(max = 200, message = "Nome deve ter no máximo 200 caracteres.")
    private String nome;

    @Email(message = "Informe um e-mail válido.")
    @Size(max = 200, message = "E-mail deve ter no máximo 200 caracteres.")
    private String email;

    @Size(max = 30, message = "Telefone deve ter no máximo 30 caracteres.")
    private String telefone;

    @Size(max = 100, message = "Origem deve ter no máximo 100 caracteres.")
    private String origem;

    @Size(max = 100, message = "Campanha deve ter no máximo 100 caracteres.")
    private String campanha;

    private String mensagem;

    private final Map<String, Object> extras = new LinkedHashMap<>();

    @JsonAnySetter
    public void setExtra(String key, Object value) {
        extras.put(key, value);
    }

    public String getExtrasJson() {
        if (extras.isEmpty()) return null;
        try {
            return new ObjectMapper().writeValueAsString(extras);
        } catch (Exception e) {
            return null;
        }
    }

    public String getNome()              { return nome; }
    public void setNome(String nome)     { this.nome = nome; }
    public String getEmail()             { return email; }
    public void setEmail(String email)   { this.email = email; }
    public String getTelefone()          { return telefone; }
    public void setTelefone(String v)    { this.telefone = v; }
    public String getOrigem()            { return origem; }
    public void setOrigem(String v)      { this.origem = v; }
    public String getCampanha()          { return campanha; }
    public void setCampanha(String v)    { this.campanha = v; }
    public String getMensagem()          { return mensagem; }
    public void setMensagem(String v)    { this.mensagem = v; }
}
