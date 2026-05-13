package com.syntra.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CriarUsuarioDTO {

    @NotBlank(message = "Informe o nome completo.")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
    private String nome;

    @NotBlank(message = "Informe o e-mail.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 120, message = "O e-mail deve ter no máximo 120 caracteres.")
    private String email;

    @NotBlank(message = "Informe a senha.")
    @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres.")
    private String senha;

    @NotBlank(message = "Selecione um perfil.")
    @Pattern(regexp = "ADMIN|VENDEDOR", message = "Perfil inválido.")
    private String perfil;

    public String getNome()              { return nome; }
    public void setNome(String nome)     { this.nome = nome; }
    public String getEmail()             { return email; }
    public void setEmail(String email)   { this.email = email; }
    public String getSenha()             { return senha; }
    public void setSenha(String senha)   { this.senha = senha; }
    public String getPerfil()            { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }
}
