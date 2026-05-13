package com.syntra.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(
            @RequestParam(required = false) String erro,
            @RequestParam(required = false) String saiu,
            Model model) {

        if (erro != null) model.addAttribute("erro", "Email ou senha incorretos.");
        if (saiu != null) model.addAttribute("info", "Você saiu com sucesso.");

        return "auth/login";
    }
}
