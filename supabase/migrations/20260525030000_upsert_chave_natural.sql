-- Unique constraint para upsert por chave natural.
-- NULLS NOT DISTINCT (PG15+) trata codigo_conta NULL como igual a NULL,
-- evitando duplicatas em linhas sem código.
alter table public.linha_demonstrativo
  drop constraint if exists uq_linha_chave_natural;

alter table public.linha_demonstrativo
  add constraint uq_linha_chave_natural
  unique nulls not distinct (id_empresa, tipo, codigo_conta, descricao, periodo);
