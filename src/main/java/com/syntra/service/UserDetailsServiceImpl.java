package com.syntra.service;

import com.syntra.model.Usuario;
import com.syntra.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + normalizedEmail));

        if (!usuario.isAtivo()) {
            throw new UsernameNotFoundException("Usuário inativo: " + normalizedEmail);
        }

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getSenha())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name())))
                .build();
    }
}
