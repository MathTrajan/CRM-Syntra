package com.syntra.dto;

import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class AtualizarFollowUpDTO {

    @Size(max = 255, message = "A próxima ação deve ter no máximo 255 caracteres.")
    private String proximaAcao;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime proximoContatoEm;

    public String getProximaAcao() { return proximaAcao; }
    public void setProximaAcao(String proximaAcao) { this.proximaAcao = proximaAcao; }
    public LocalDateTime getProximoContatoEm() { return proximoContatoEm; }
    public void setProximoContatoEm(LocalDateTime proximoContatoEm) { this.proximoContatoEm = proximoContatoEm; }
}
