package com.syntra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AtualizarPerfilDTO {

    @NotBlank(message = "Informe seu nome.")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
    private String nome;

    private boolean receberLembretes = true;
    private boolean resumoDiario;
    private boolean timelineCompacta;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public boolean isReceberLembretes() { return receberLembretes; }
    public void setReceberLembretes(boolean receberLembretes) { this.receberLembretes = receberLembretes; }
    public boolean isResumoDiario() { return resumoDiario; }
    public void setResumoDiario(boolean resumoDiario) { this.resumoDiario = resumoDiario; }
    public boolean isTimelineCompacta() { return timelineCompacta; }
    public void setTimelineCompacta(boolean timelineCompacta) { this.timelineCompacta = timelineCompacta; }
}
