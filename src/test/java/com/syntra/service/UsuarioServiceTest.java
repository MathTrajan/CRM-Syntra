package com.syntra.service;

import com.syntra.dto.CriarUsuarioDTO;
import com.syntra.model.Usuario;
import com.syntra.model.enums.Perfil;
import com.syntra.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private CriarUsuarioDTO criarUsuarioDTO;

    @BeforeEach
    void setUp() {
        criarUsuarioDTO = new CriarUsuarioDTO();
        criarUsuarioDTO.setNome("Maria Silva");
        criarUsuarioDTO.setEmail("maria@syntra.com");
        criarUsuarioDTO.setSenha("segredo123");
        criarUsuarioDTO.setPerfil("VENDEDOR");
    }

    @Test
    void criarDeveNormalizarEmailESalvarSenhaCriptografada() {
        when(usuarioRepository.findByEmail("maria@syntra.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("segredo123")).thenReturn("hash");

        usuarioService.criar(criarUsuarioDTO);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario salvo = captor.getValue();
        assertEquals("Maria Silva", salvo.getNome());
        assertEquals("maria@syntra.com", salvo.getEmail());
        assertEquals("hash", salvo.getSenha());
        assertEquals(Perfil.VENDEDOR, salvo.getPerfil());
    }

    @Test
    void redefinirSenhaDevePersistirNovaSenhaCriptografada() {
        Usuario usuario = new Usuario();
        usuario.setSenha("antiga");

        when(usuarioRepository.findById("1")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("novaSenha123")).thenReturn("hashNova");

        usuarioService.redefinirSenha("1", "novaSenha123");

        assertEquals("hashNova", usuario.getSenha());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void alterarSenhaPropriaDeveValidarSenhaAtual() {
        Usuario usuario = new Usuario();
        usuario.setEmail("maria@syntra.com");
        usuario.setSenha("hashAtual");

        when(usuarioRepository.findByEmail("maria@syntra.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaErrada", "hashAtual")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.alterarSenhaPropria("maria@syntra.com", "senhaErrada", "novaSenha123"));

        assertEquals("A senha atual está incorreta.", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }
}
