-- Patch do code review do PR #34.
-- A migration 20260512000000_create_usuario.sql foi aplicada em uma versao
-- anterior que continha policies de INSERT em usuario/empresa e nao chamava
-- enable row level security explicitamente. Esta migration aplica o delta
-- ao banco remoto:
--   1. garante RLS ligada em ambas as tabelas
--   2. remove as policies de INSERT (linha so deve ser criada pela trigger
--      handle_new_user, com SECURITY DEFINER)

alter table public.usuario enable row level security;
alter table public.empresa enable row level security;

drop policy if exists "usuario_insert_own" on public.usuario;
drop policy if exists "empresa_insert_own" on public.empresa;
