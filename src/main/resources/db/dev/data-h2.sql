-- Seed minimo para perfil dev local.
-- O hash bcrypt corresponde a senha entregue fora-de-banda ao operador.

INSERT INTO usuario (id, nome, email, senha, perfil, ativo) VALUES
('u-admin', 'Administrador', 'admin@syntra.local', '$2b$10$ZHpcM..nyBr3Hv6hRAWTaOtvk7CZ/dPOXxCJIMl5x2srjhSmz91Yq', 'ADMIN', TRUE);
