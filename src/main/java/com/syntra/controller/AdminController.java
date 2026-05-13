package com.syntra.controller;

import com.syntra.dto.CriarUsuarioDTO;
import com.syntra.dto.ResetSenhaDTO;
import com.syntra.model.enums.Perfil;
import com.syntra.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UsuarioService usuarioService;

    public AdminController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @ModelAttribute("criarUsuarioForm")
    public CriarUsuarioDTO criarUsuarioForm() {
        return new CriarUsuarioDTO();
    }

    @ModelAttribute("resetSenhaForm")
    public ResetSenhaDTO resetSenhaForm() {
        return new ResetSenhaDTO();
    }

    @GetMapping("/usuarios")
    public String listar(@RequestParam(defaultValue = "0") int page, Model model) {
        popularPagina(model, page);
        return "admin/usuarios";
    }

    @PostMapping("/usuarios")
    public String criar(@Valid @ModelAttribute("criarUsuarioForm") CriarUsuarioDTO dto,
                        BindingResult bindingResult,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            popularPagina(model, 0);
            model.addAttribute("modalAberto", "criar");
            return "admin/usuarios";
        }

        try {
            usuarioService.criar(dto);
            redirectAttributes.addFlashAttribute("sucesso",
                    "Usuário \"" + dto.getNome().trim() + "\" criado com sucesso.");
            return "redirect:/admin/usuarios";
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("email", "duplicado", e.getMessage());
            popularPagina(model, 0);
            model.addAttribute("modalAberto", "criar");
            return "admin/usuarios";
        }
    }

    @PostMapping("/usuarios/{id}/toggle")
    public String toggleAtivo(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            usuarioService.toggleAtivo(id);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/reset-senha")
    public String resetarSenha(@PathVariable String id,
                               @Valid @ModelAttribute("resetSenhaForm") ResetSenhaDTO dto,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            popularPagina(model, 0);
            prepararResetSenha(model, id);
            return "admin/usuarios";
        }

        try {
            usuarioService.redefinirSenha(id, dto.getNovaSenha());
            redirectAttributes.addFlashAttribute("sucesso", "Senha redefinida com sucesso.");
            return "redirect:/admin/usuarios";
        } catch (IllegalArgumentException e) {
            bindingResult.reject("resetSenha", e.getMessage());
            popularPagina(model, 0);
            prepararResetSenha(model, id);
            return "admin/usuarios";
        }
    }

    private void popularPagina(Model model, int page) {
        var pagina = usuarioService.listar(PageRequest.of(page, 10, Sort.by("nome")));
        model.addAttribute("usuarios", pagina.getContent());
        model.addAttribute("paginaAtual", pagina.getNumber());
        model.addAttribute("totalPaginas", pagina.getTotalPages());
        model.addAttribute("totalUsuarios", pagina.getTotalElements());
        model.addAttribute("perfis", Perfil.values());
    }

    private void prepararResetSenha(Model model, String id) {
        model.addAttribute("modalAberto", "reset");
        model.addAttribute("resetSenhaUsuario", usuarioService.buscarPorId(id));
    }
}
