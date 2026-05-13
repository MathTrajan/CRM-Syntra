package com.syntra.dto.divia;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class DiviaLeadDTO {

    private String id;

    @JsonProperty("Qualificação")
    private String qualificacao;

    @JsonProperty("Origem")
    private String origem;

    @JsonProperty("Tipo")
    private String tipo;

    @JsonProperty("Nome")
    private String nome;

    @JsonProperty("E-mail")
    private String email;

    @JsonProperty("Telefone")
    private String telefone;

    @JsonProperty("Mensagem")
    private String mensagem;

    private String referrer;

    @JsonProperty("Palavra-chave")
    private String palavraChave;

    @JsonProperty("Anúncio")
    private String anuncio;

    @JsonProperty("Interesse")
    private String interesse;

    @JsonProperty("Dados Adicionais")
    private Map<String, Object> dadosAdicionais;

    @JsonProperty("Data")
    private String data;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getQualificacao() { return qualificacao; }
    public void setQualificacao(String qualificacao) { this.qualificacao = qualificacao; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
    public String getReferrer() { return referrer; }
    public void setReferrer(String referrer) { this.referrer = referrer; }
    public String getPalavraChave() { return palavraChave; }
    public void setPalavraChave(String palavraChave) { this.palavraChave = palavraChave; }
    public String getAnuncio() { return anuncio; }
    public void setAnuncio(String anuncio) { this.anuncio = anuncio; }
    public String getInteresse() { return interesse; }
    public void setInteresse(String interesse) { this.interesse = interesse; }
    public Map<String, Object> getDadosAdicionais() { return dadosAdicionais; }
    public void setDadosAdicionais(Map<String, Object> dadosAdicionais) { this.dadosAdicionais = dadosAdicionais; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
}
