package com.syntra.controller;

import com.syntra.dto.DashboardFiltroDTO;
import com.syntra.model.Usuario;
import com.syntra.service.AlertaService;
import com.syntra.service.DashboardService;
import com.syntra.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final UsuarioService usuarioService;
    private final AlertaService alertaService;

    public DashboardController(DashboardService dashboardService,
                               UsuarioService usuarioService,
                               AlertaService alertaService) {
        this.dashboardService = dashboardService;
        this.usuarioService   = usuarioService;
        this.alertaService    = alertaService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(DashboardFiltroDTO filtro,
                            Model model,
                            Authentication authentication) {
        model.addAttribute("metricas", dashboardService.getMetricas(filtro));
        model.addAttribute("filtro", filtro);
        model.addAttribute("paginaAtiva", "dashboard");

        // Alertas por vendedor: exibe apenas alertas do usuário logado
        if (authentication != null) {
            Usuario usuario = usuarioService.buscarPorEmail(authentication.getName());
            model.addAttribute("alertas",
                    usuario != null
                            ? alertaService.alertasDoVendedor(usuario.getId())
                            : java.util.List.of());
        } else {
            model.addAttribute("alertas", java.util.List.of());
        }

        return "dashboard/index";
    }
}
