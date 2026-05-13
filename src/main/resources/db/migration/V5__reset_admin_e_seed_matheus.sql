-- V5: Corrige o hash bcrypt do administrador padrao (o hash original em V2 nao
--     correspondia a credencial documentada e impedia o login) e adiciona um
--     novo usuario administrador a partir de um seed parametrizado em build.
--
-- Hashes BCrypt cost 10. As credenciais em texto NAO devem ficar versionadas
-- no repositorio publico - elas sao entregues fora-de-banda ao operador.

UPDATE usuario
   SET senha = '$2b$10$EtCf8XIkBJKkSHaS9j8mKOX12QuzwI5yogDzWXSyKLzpaJBg9AL4i',
       atualizado_em = NOW()
 WHERE email = 'admin@syntra.com';

INSERT INTO usuario (id, nome, email, senha, perfil, ativo)
VALUES (
    gen_random_uuid()::text,
    'Matheus Ferreira',
    'matheus.ferreira@gmail.com',
    '$2b$10$KKaG3TocBveu0Q30zWvVyuZyfotUz282hq/CnYpYXHDXU0v4g7Boe',
    'ADMIN',
    TRUE
) ON CONFLICT (email) DO UPDATE
   SET senha  = EXCLUDED.senha,
       perfil = EXCLUDED.perfil,
       ativo  = TRUE,
       atualizado_em = NOW();
