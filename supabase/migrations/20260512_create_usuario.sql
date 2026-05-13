-- Suporte ao fluxo de cadastro com Supabase Auth.
-- Mudanças aditivas: nova coluna em empresa + policies de RLS.
-- Não altera constraints existentes nem dropa colunas.

alter table public.empresa
  add column if not exists razao_social text;

-- Policies para a tabela usuario (RLS já está ligada).
-- Permitem que o próprio usuário leia/insira/atualize sua linha.
drop policy if exists "usuario_select_own" on public.usuario;
create policy "usuario_select_own"
  on public.usuario for select
  to authenticated
  using (auth.uid() = id_usuario);

drop policy if exists "usuario_insert_own" on public.usuario;
create policy "usuario_insert_own"
  on public.usuario for insert
  to authenticated
  with check (auth.uid() = id_usuario);

drop policy if exists "usuario_update_own" on public.usuario;
create policy "usuario_update_own"
  on public.usuario for update
  to authenticated
  using (auth.uid() = id_usuario)
  with check (auth.uid() = id_usuario);

-- Policies para a tabela empresa (RLS já está ligada).
drop policy if exists "empresa_select_own" on public.empresa;
create policy "empresa_select_own"
  on public.empresa for select
  to authenticated
  using (auth.uid() = id_usuario);

drop policy if exists "empresa_insert_own" on public.empresa;
create policy "empresa_insert_own"
  on public.empresa for insert
  to authenticated
  with check (auth.uid() = id_usuario);

drop policy if exists "empresa_update_own" on public.empresa;
create policy "empresa_update_own"
  on public.empresa for update
  to authenticated
  using (auth.uid() = id_usuario)
  with check (auth.uid() = id_usuario);
