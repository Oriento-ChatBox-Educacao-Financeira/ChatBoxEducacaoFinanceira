-- Suporte ao fluxo de cadastro com Supabase Auth.
-- Mudanças aditivas: nova coluna em empresa + policies de RLS.
-- Não altera constraints existentes nem dropa colunas.
--
-- Observação: as tabelas `public.usuario` e `public.empresa` são criadas pela
-- migration base do schema (fora desta PR). Esta migration é defensiva quanto
-- a colunas/policies/RLS e idempotente.

alter table public.empresa
  add column if not exists razao_social text;

-- Garante RLS ligada explicitamente nas duas tabelas. Sem isso, em um setup
-- novo as policies abaixo não teriam efeito.
alter table public.usuario enable row level security;
alter table public.empresa enable row level security;

-- Policies para a tabela usuario.
-- Permitem que o próprio usuário leia/atualize sua linha.
-- INSERT não é exposto a clientes: a linha é criada pela trigger
-- `handle_new_user` (SECURITY DEFINER) durante o sign-up, evitando que o
-- cliente autenticado consiga inserir registros arbitrários.
drop policy if exists "usuario_select_own" on public.usuario;
create policy "usuario_select_own"
  on public.usuario for select
  to authenticated
  using (auth.uid() = id_usuario);

drop policy if exists "usuario_insert_own" on public.usuario;

drop policy if exists "usuario_update_own" on public.usuario;
create policy "usuario_update_own"
  on public.usuario for update
  to authenticated
  using (auth.uid() = id_usuario)
  with check (auth.uid() = id_usuario);

-- Policies para a tabela empresa.
-- Mesma lógica: SELECT/UPDATE pelo dono, INSERT só via trigger.
drop policy if exists "empresa_select_own" on public.empresa;
create policy "empresa_select_own"
  on public.empresa for select
  to authenticated
  using (auth.uid() = id_usuario);

drop policy if exists "empresa_insert_own" on public.empresa;

drop policy if exists "empresa_update_own" on public.empresa;
create policy "empresa_update_own"
  on public.empresa for update
  to authenticated
  using (auth.uid() = id_usuario)
  with check (auth.uid() = id_usuario);
