package com.syntra.controller;

import com.syntra.dto.DashboardFiltroDTO;
import com.syntra.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(DashboardFiltroDTO filtro, Model model) {
        model.addAttribute("metricas", dashboardService.getMetricas(filtro));
        model.addAttribute("filtro", filtro);
        model.addAttribute("paginaAtiva", "dashboard");
        return "dashboard/index";
    }
}
