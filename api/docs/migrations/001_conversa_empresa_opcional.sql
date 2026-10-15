-- Executar no PostgreSQL do Oriento antes de habilitar o proxy.
-- Preserva as conversas e seus IDs; pode ser executado novamente.
BEGIN;
ALTER TABLE conversa ALTER COLUMN id_empresa DROP NOT NULL;
COMMIT;
