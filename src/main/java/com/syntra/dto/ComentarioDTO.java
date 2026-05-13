package com.syntra.dto;

import jakarta.validation.constraints.NotBlank;

public class ComentarioDTO {

    @NotBlank(message = "Texto do comentário não pode ser vazio")
    private String texto;

    public String getTexto()           { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
}
