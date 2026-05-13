package com.syntra.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetSenhaDTO {

    @NotBlank(message = "Informe a nova senha.")
    @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres.")
    private String novaSenha;

    @NotBlank(message = "Confirme a nova senha.")
    private String confirmarSenha;

    @AssertTrue(message = "A confirmação de senha não confere.")
    public boolean isSenhaConfirmada() {
        if (novaSenha == null || confirmarSenha == null) {
            return true;
        }
        return novaSenha.equals(confirmarSenha);
    }

    public String getNovaSenha() {
        return novaSenha;
    }

    public void setNovaSenha(String novaSenha) {
        this.novaSenha = novaSenha;
    }

    public String getConfirmarSenha() {
        return confirmarSenha;
    }

    public void setConfirmarSenha(String confirmarSenha) {
        this.confirmarSenha = confirmarSenha;
    }
}
