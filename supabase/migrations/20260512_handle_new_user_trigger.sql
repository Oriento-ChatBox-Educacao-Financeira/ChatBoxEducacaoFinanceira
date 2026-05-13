-- Trigger que materializa usuario + empresa quando um auth.users é criado.
-- Lê os campos enviados em raw_user_meta_data pelo signUp do frontend.
-- Roda com SECURITY DEFINER para bypassar o RLS no momento do insert.

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.usuario (id_usuario, nome, email, senha_hash)
  values (
    new.id,
    coalesce(new.raw_user_meta_data->>'nome', new.email),
    new.email,
    'managed_by_supabase'
  );

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
    );
  end if;

  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();
