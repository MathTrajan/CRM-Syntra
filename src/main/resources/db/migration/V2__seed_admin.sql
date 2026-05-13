-- Usuário admin padrão (senha: admin123 em BCrypt)
-- Trocar a senha via painel ou variável de ambiente em produção
INSERT INTO usuario (id, nome, email, senha, perfil, ativo)
VALUES (
    gen_random_uuid()::text,
    'Administrador',
    'admin@syntra.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBaIlKGLFNLPGy',
    'ADMIN',
    TRUE
) ON CONFLICT (email) DO NOTHING;
-- Hash acima corresponde a 'admin123'
