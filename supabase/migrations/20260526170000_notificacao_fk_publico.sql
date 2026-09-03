-- =====================================================
-- Migration: realinha FK de notificacao apos saida do Supabase Auth
-- =====================================================
-- Contexto: a tabela `public.notificacao` foi criada com
-- `id_usuario uuid references auth.users(id)`. Apos a migracao do
-- modulo de autenticacao para o backend Java (JWT proprio), os
-- usuarios sao criados diretamente em `public.usuario` e nao em
-- `auth.users`. A FK antiga passou a quebrar todo INSERT de
-- notificacao com o erro:
--   "ERROR: insert or update on table 'notificacao' violates foreign
--    key constraint 'notificacao_id_usuario_fkey'.
--    Key (id_usuario)=(<uuid>) is not present in table 'users'."
--
-- Esta migration:
--   1. Dropa a FK antiga (apontava para auth.users).
--   2. Recria apontando para public.usuario(id_usuario) ON DELETE CASCADE.
-- Idempotente: nao falha se rodada novamente.

alter table public.notificacao
    drop constraint if exists notificacao_id_usuario_fkey;

alter table public.notificacao
    add constraint notificacao_id_usuario_fkey
    foreign key (id_usuario)
    references public.usuario(id_usuario)
    on delete cascade;
