-- Tabela de notificações por usuário
create table if not exists public.notificacao (
    id_notificacao bigint generated always as identity primary key,
    id_usuario     uuid not null references auth.users(id) on delete cascade,
    tipo           text not null check (tipo in ('PLANILHA_INCOMPLETA','PLANILHA_IMPORTADA','GERAL','SISTEMA')),
    titulo         text not null,
    mensagem       text not null,
    link           text,
    icone          text,
    lida           boolean not null default false,
    data_criacao   timestamptz not null default now(),
    data_leitura   timestamptz
);

create index if not exists idx_notif_user_unread
    on public.notificacao (id_usuario, lida, data_criacao desc);

alter table public.notificacao enable row level security;

drop policy if exists "notif_select_own" on public.notificacao;
create policy "notif_select_own" on public.notificacao
  for select using (id_usuario = auth.uid());

drop policy if exists "notif_insert_own" on public.notificacao;
create policy "notif_insert_own" on public.notificacao
  for insert with check (id_usuario = auth.uid());

drop policy if exists "notif_update_own" on public.notificacao;
create policy "notif_update_own" on public.notificacao
  for update using (id_usuario = auth.uid())
  with check (id_usuario = auth.uid());

drop policy if exists "notif_delete_own" on public.notificacao;
create policy "notif_delete_own" on public.notificacao
  for delete using (id_usuario = auth.uid());
