package com.syntra.config;

import com.syntra.model.Lead;
import com.syntra.model.Usuario;
import com.syntra.model.enums.Perfil;
import com.syntra.model.enums.StatusLead;
import com.syntra.repository.LeadRepository;
import com.syntra.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

/**
 * Seed minimo para o perfil dev local: garante que exista um administrador
 * com as credenciais definidas em ${ADMIN_EMAIL} / ${ADMIN_PASSWORD} e popula
 * alguns leads de exemplo (apenas se a tabela estiver vazia) para validacao
 * manual da UI (busca, atribuir vendedor, status etc).
 *
 * Em producao este bean nao roda (@Profile dev).
 */
@Configuration
@Profile("dev")
public class DataInitializer {

    @Bean
    public ApplicationRunner seedDevData(UsuarioRepository usuarioRepo,
                                         LeadRepository leadRepo,
                                         PasswordEncoder encoder,
                                         @Value("${syntra.admin.email:admin@syntra.local}") String adminEmail,
                                         @Value("${syntra.admin.password:trocar-no-primeiro-login}") String adminPassword) {
        return args -> {
            Usuario admin = usuarioRepo.findByEmail(adminEmail).orElseGet(() -> {
                Usuario u = new Usuario();
                u.setNome("Administrador");
                u.setEmail(adminEmail);
                u.setSenha(encoder.encode(adminPassword));
                u.setPerfil(Perfil.ADMIN);
                return usuarioRepo.save(u);
            });

            if (leadRepo.count() > 0) {
                return;
            }

            LocalDateTime agora = LocalDateTime.now();

            // Variedade proposital de origens, status e dadosExtras para
            // exercitar a busca em todos os campos.
            criarLead(leadRepo, "Joana Aparecida da Silva", "joana.silva@gmail.com",
                    "(11) 98877-1234", "Facebook Ads", "remarketing-maio",
                    "Quero saber valores do plano premium", StatusLead.NOVO, false,
                    "{\"interesse\":\"plano premium\",\"anuncio\":\"vid-promo-mae\"}",
                    agora.minusHours(2));

            criarLead(leadRepo, "Carlos Eduardo Pinheiro", "carlos.ep@hotmail.com",
                    "11955667788", "Google Ads", "branding-junho",
                    null, StatusLead.EM_ATENDIMENTO, true,
                    "{\"interesse\":\"colchao casal\",\"qualificacao\":\"morno\"}",
                    agora.minusHours(8));

            criarLead(leadRepo, "Mariana Costa Lima", "mari.cl@outlook.com",
                    "+55 21 9 9911-2233", "WhatsApp", null,
                    "Cliente indicada por Ana", StatusLead.AGUARDANDO_RETORNO, true,
                    "{\"referrer\":\"site oficial\",\"tipo\":\"indicacao\"}",
                    agora.minusDays(1));

            criarLead(leadRepo, "Ricardo Tavares", "ricardotavares92@gmail.com",
                    "21988774422", "Instagram", "stories-promocao",
                    "Falou que vai pensar", StatusLead.PERDIDO, true,
                    "{\"palavraChave\":\"colchao king\",\"qualificacao\":\"frio\"}",
                    agora.minusDays(3));

            criarLead(leadRepo, "Beatriz Almeida", null,
                    "(85) 9 8765-4321", "Organico", null,
                    "Compra fechada via WhatsApp", StatusLead.CONVERTIDO, true,
                    "{\"qualificacao\":\"quente\",\"interesse\":\"colchao king + travesseiros\"}",
                    agora.minusDays(5));

            criarLead(leadRepo, "Pedro Henrique", "pedro.h@empresa.com.br",
                    "11 4002-8922", "LinkedIn", "b2b-corporativo",
                    "Solicitou orcamento corporativo de 200 colchoes", StatusLead.NOVO, false,
                    "{\"tipo\":\"B2B\",\"qualificacao\":\"quente\",\"interesse\":\"corporativo\"}",
                    agora.minusMinutes(45));
        };
    }

    private void criarLead(LeadRepository repo, String nome, String email, String telefone,
                           String origem, String campanha, String mensagem,
                           StatusLead status, boolean lido,
                           String dadosExtras, LocalDateTime criadoEm) {
        Lead l = new Lead();
        l.setNome(nome);
        l.setEmail(email);
        l.setTelefone(telefone);
        l.setOrigem(origem);
        l.setCampanha(campanha);
        l.setMensagem(mensagem);
        l.setStatus(status);
        l.setLido(lido);
        l.setDadosExtras(dadosExtras);
        l.setCriadoEm(criadoEm);
        l.setUltimaInteracaoEm(criadoEm);
        repo.save(l);
    }
}
