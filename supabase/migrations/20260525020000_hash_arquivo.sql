-- Adiciona hash do arquivo importado para detecção de duplicatas
alter table public.linha_demonstrativo
  add column if not exists hash_arquivo text;

create index if not exists idx_linha_empresa_hash
  on public.linha_demonstrativo (id_empresa, hash_arquivo)
  where hash_arquivo is not null;
