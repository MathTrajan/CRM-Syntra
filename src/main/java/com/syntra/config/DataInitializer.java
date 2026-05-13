package com.syntra.config;

import com.syntra.model.Usuario;
import com.syntra.model.enums.Perfil;
import com.syntra.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seed minimo para o perfil dev: garante apenas que exista um administrador
 * com as credenciais definidas em ${ADMIN_EMAIL} / ${ADMIN_PASSWORD}.
 *
 * Nao popula leads de exemplo - todos os dados em prod vem do webhook ou da
 * integracao externa. Para popular dados de teste, use a propria interface
 * (POST /api/webhook) com o WEBHOOK_SECRET local.
 */
@Configuration
@Profile("dev")
public class DataInitializer {

    @Bean
    public ApplicationRunner seedDevData(UsuarioRepository usuarioRepo,
                                         PasswordEncoder encoder,
                                         @Value("${syntra.admin.email:admin@syntra.local}") String adminEmail,
                                         @Value("${syntra.admin.password:trocar-no-primeiro-login}") String adminPassword) {
        return args -> {
            if (usuarioRepo.findByEmail(adminEmail).isPresent()) {
                return;
            }
            Usuario admin = new Usuario();
            admin.setNome("Administrador");
            admin.setEmail(adminEmail);
            admin.setSenha(encoder.encode(adminPassword));
            admin.setPerfil(Perfil.ADMIN);
            usuarioRepo.save(admin);
        };
    }
}
