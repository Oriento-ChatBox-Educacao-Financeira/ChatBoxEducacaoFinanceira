-- =====================================================
-- Storage: bucket 'planilhas' + policies
-- =====================================================
insert into storage.buckets (id, name, public)
values ('planilhas', 'planilhas', false)
on conflict (id) do nothing;

-- Convenção: arquivos são gravados em <auth.uid()>/<arquivo.ext>
-- Assim a primeira pasta funciona como namespace por usuário.

drop policy if exists "planilhas_owner_select" on storage.objects;
create policy "planilhas_owner_select"
on storage.objects for select
to authenticated
using (
    bucket_id = 'planilhas'
    and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists "planilhas_owner_insert" on storage.objects;
create policy "planilhas_owner_insert"
on storage.objects for insert
to authenticated
with check (
    bucket_id = 'planilhas'
    and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists "planilhas_owner_update" on storage.objects;
create policy "planilhas_owner_update"
on storage.objects for update
to authenticated
using (
    bucket_id = 'planilhas'
    and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists "planilhas_owner_delete" on storage.objects;
create policy "planilhas_owner_delete"
on storage.objects for delete
to authenticated
using (
    bucket_id = 'planilhas'
    and (storage.foldername(name))[1] = auth.uid()::text
);
