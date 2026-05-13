package com.syntra.service;

import com.syntra.dto.AtualizarPerfilDTO;
import com.syntra.dto.CriarUsuarioDTO;
import com.syntra.model.Usuario;
import com.syntra.model.enums.Perfil;
import com.syntra.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<Usuario> listar(Pageable pageable) {
        return usuarioRepository.findAll(pageable);
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll(Sort.by("nome"));
    }

    public void criar(CriarUsuarioDTO dto) {
        String email = dto.getEmail().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("E-mail ja cadastrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(dto.getNome().trim());
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setPerfil(parsePerfil(dto.getPerfil()));
        usuarioRepository.save(usuario);
    }

    public void toggleAtivo(String id) {
        Usuario usuario = buscarPorId(id);
        usuario.setAtivo(!usuario.isAtivo());
        usuarioRepository.save(usuario);
    }

    public void redefinirSenha(String id, String novaSenha) {
        Usuario usuario = buscarPorId(id);
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
    }

    public void alterarSenhaPropria(String email, String senhaAtual, String novaSenha) {
        Usuario usuario = buscarPorEmail(email);

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw new IllegalArgumentException("A senha atual esta incorreta.");
        }

        if (passwordEncoder.matches(novaSenha, usuario.getSenha())) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da senha atual.");
        }

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
    }

    public void atualizarPerfil(String email, AtualizarPerfilDTO dto) {
        Usuario usuario = buscarPorEmail(email);
        usuario.setNome(dto.getNome().trim());
        usuario.setReceberLembretes(dto.isReceberLembretes());
        usuario.setResumoDiario(dto.isResumoDiario());
        usuario.setTimelineCompacta(dto.isTimelineCompacta());
        usuarioRepository.save(usuario);
    }

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado."));
    }

    public Usuario buscarPorId(String id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado."));
    }

    private Perfil parsePerfil(String perfil) {
        try {
            return Perfil.valueOf(perfil.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalArgumentException("Perfil invalido.");
        }
    }
}
