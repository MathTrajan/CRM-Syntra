package com.syntra.controller.api;

import com.syntra.dto.AlertaLead;
import com.syntra.service.AlertaService;
import com.syntra.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Endpoint REST que expõe os alertas do vendedor logado.
 * Usado pelo sino de notificações da navbar via polling JS.
 *
 * Escopo estritamente por vendedor: apenas leads do usuário autenticado.
 */
@RestController
@RequestMapping("/api/alertas")
public class AlertaApiController {

    private final AlertaService alertaService;
    private final UsuarioService usuarioService;

    public AlertaApiController(AlertaService alertaService, UsuarioService usuarioService) {
        this.alertaService = alertaService;
        this.usuarioService = usuarioService;
    }

    /**
     * Retorna os alertas ativos do vendedor logado.
     * Responde com {"total": N, "alertas": [...]} para o sino da navbar.
     */
    @GetMapping
    public Map<String, Object> meusAlertas(Authentication auth) {
        // Resolve o vendedor pelo e-mail do principal autenticado
        var usuario = auth != null ? usuarioService.buscarPorEmail(auth.getName()) : null;
        List<AlertaLead> alertas = usuario != null
                ? alertaService.alertasDoVendedor(usuario.getId())
                : List.of();
        return Map.of("total", alertas.size(), "alertas", alertas);
    }
}
