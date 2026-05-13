-- V8: Reseta o hash bcrypt do unico administrador para garantir a senha
-- combinada com o operador. Idempotente: se o usuario nao existir, cria;
-- caso exista, apenas atualiza a senha e o perfil.

INSERT INTO usuario (id, nome, email, senha, perfil, ativo)
VALUES (
    gen_random_uuid()::text,
    'Matheus Trajano',
    'matheustrajano.dev@gmail.com',
    '$2b$10$V2HwR6cerFEeGj1wyDZS..Xm31hu9prcwPxjxA1K7shAYolA7Jyxi',
    'ADMIN',
    TRUE
)
ON CONFLICT (email) DO UPDATE
   SET senha  = EXCLUDED.senha,
       perfil = EXCLUDED.perfil,
       ativo  = TRUE,
       atualizado_em = NOW();
