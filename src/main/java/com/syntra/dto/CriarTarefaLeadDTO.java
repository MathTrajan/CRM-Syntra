package com.syntra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class CriarTarefaLeadDTO {

    @NotBlank(message = "Informe o título da tarefa.")
    @Size(max = 160, message = "O título deve ter no máximo 160 caracteres.")
    private String titulo;

    @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres.")
    private String descricao;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime vencimentoEm;

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public LocalDateTime getVencimentoEm() { return vencimentoEm; }
    public void setVencimentoEm(LocalDateTime vencimentoEm) { this.vencimentoEm = vencimentoEm; }
}
