-- Trigger que materializa usuario + empresa quando um auth.users é criado.
-- Lê os campos enviados em raw_user_meta_data pelo signUp do frontend.
-- Roda com SECURITY DEFINER para bypassar o RLS no momento do insert.
--
-- Hardening:
-- - `search_path` fixado em `public, pg_temp` para evitar resolução
--   inesperada de nomes em schemas controlados pelo chamador.
-- - EXECUTE revogado de PUBLIC: só o owner e o postgres podem invocar a
--   função diretamente; a trigger continua funcionando porque é executada
--   no contexto do owner.
-- - INSERTs idempotentes via ON CONFLICT DO NOTHING: replays/restores não
--   quebram a criação do auth.user.
-- - `senha_hash` recebe NULL: a senha é gerenciada inteiramente pelo
--   Supabase Auth; valores literais como 'managed_by_supabase' eram
--   confusos e podiam induzir uso indevido da coluna.

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
  insert into public.usuario (id_usuario, nome, email, senha_hash)
  values (
    new.id,
    coalesce(new.raw_user_meta_data->>'nome', new.email),
    new.email,
    null
  )
  on conflict (id_usuario) do nothing;

  if new.raw_user_meta_data->>'nome_fantasia' is not null then
    insert into public.empresa (
      id_usuario, nome_fantasia, cnpj, razao_social, regime_tributario
    )
    values (
      new.id,
      new.raw_user_meta_data->>'nome_fantasia',
      new.raw_user_meta_data->>'cnpj',
      new.raw_user_meta_data->>'razao_social',
      'Simples Nacional'
    )
    on conflict (id_usuario) do nothing;
  end if;

  return new;
end;
$$;

revoke execute on function public.handle_new_user() from public;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();
