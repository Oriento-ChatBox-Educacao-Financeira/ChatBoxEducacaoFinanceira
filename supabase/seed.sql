-- Seed local — opcional.
-- O fluxo normal é: você cadastra um usuário pela tela /register do app.
-- A trigger handle_new_user (criada em 20260523_local_baseline.sql) já popula
-- public.usuario e public.empresa automaticamente.
--
-- Este arquivo é só um diagnóstico — mostra contagens das tabelas após
-- o supabase start (ou supabase db reset).

do $$
declare
    v_users  int;
    v_perfil int;
    v_emp    int;
begin
    select count(*) into v_users  from auth.users;
    select count(*) into v_perfil from public.usuario;
    select count(*) into v_emp    from public.empresa;
    raise notice 'auth.users=% | public.usuario=% | public.empresa=%', v_users, v_perfil, v_emp;
end$$;
