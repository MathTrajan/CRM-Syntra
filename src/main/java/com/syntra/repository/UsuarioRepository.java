package com.syntra.repository;

import com.syntra.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    Optional<Usuario> findByEmail(String email);

    List<Usuario> findByAtivoTrueOrderByNome();
}
