package com.syntra.controller;

import com.syntra.dto.AlterarSenhaDTO;
import com.syntra.dto.AtualizarPerfilDTO;
import com.syntra.model.Usuario;
import com.syntra.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping
public class ContaController {

    private final UsuarioService usuarioService;

    public ContaController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/conta")
    public String conta() {
        return "redirect:/perfil";
    }

    @GetMapping({"/perfil", "/senha", "/conta/senha", "/minha-senha"})
    public String perfil(Authentication authentication, Model model) {
        preencherPerfil(model, authentication, false);
        return "conta/perfil";
    }

    @PostMapping("/perfil")
    public String atualizarPerfil(@Valid @ModelAttribute("perfilForm") AtualizarPerfilDTO dto,
                                  BindingResult bindingResult,
                                  Authentication authentication,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            preencherPerfil(model, authentication, false);
            return "conta/perfil";
        }

        usuarioService.atualizarPerfil(authentication.getName(), dto);
        redirectAttributes.addFlashAttribute("sucessoPerfil", "Perfil atualizado com sucesso.");
        return "redirect:/perfil";
    }

    @PostMapping({"/senha", "/conta/senha", "/minha-senha"})
    public String alterarSenha(@Valid @ModelAttribute("alterarSenhaForm") AlterarSenhaDTO dto,
                               BindingResult bindingResult,
                               Authentication authentication,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            preencherPerfil(model, authentication, true);
            return "conta/perfil";
        }

        try {
            usuarioService.alterarSenhaPropria(authentication.getName(), dto.getSenhaAtual(), dto.getNovaSenha());
            redirectAttributes.addFlashAttribute("sucessoSenha", "Sua senha foi alterada com sucesso.");
            return "redirect:/perfil";
        } catch (IllegalArgumentException e) {
            model.addAttribute("erro", e.getMessage());
            preencherPerfil(model, authentication, true);
            return "conta/perfil";
        }
    }

    private void preencherPerfil(Model model, Authentication authentication, boolean segurancaAtiva) {
        Usuario usuario = usuarioService.buscarPorEmail(authentication.getName());

        if (!model.containsAttribute("perfilForm")) {
            AtualizarPerfilDTO perfil = new AtualizarPerfilDTO();
            perfil.setNome(usuario.getNome());
            perfil.setReceberLembretes(usuario.isReceberLembretes());
            perfil.setResumoDiario(usuario.isResumoDiario());
            perfil.setTimelineCompacta(usuario.isTimelineCompacta());
            model.addAttribute("perfilForm", perfil);
        }
        if (!model.containsAttribute("alterarSenhaForm")) {
            model.addAttribute("alterarSenhaForm", new AlterarSenhaDTO());
        }

        model.addAttribute("usuarioAtual", usuario);
        model.addAttribute("abaAtiva", segurancaAtiva ? "seguranca" : "perfil");
        model.addAttribute("paginaAtiva", "perfil");
    }
}
