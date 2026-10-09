-- Policy de UPDATE em linha_demonstrativo: necessária pra upsert funcionar
-- (ON CONFLICT DO UPDATE precisa de permissão de UPDATE, não só INSERT).
drop policy if exists "linha_demo_update_own" on public.linha_demonstrativo;
create policy "linha_demo_update_own"
on public.linha_demonstrativo for update
using (
    exists (
        select 1 from public.empresa e
        where e.id_empresa = linha_demonstrativo.id_empresa
          and e.id_usuario = auth.uid()
    )
)
with check (
    exists (
        select 1 from public.empresa e
        where e.id_empresa = linha_demonstrativo.id_empresa
          and e.id_usuario = auth.uid()
    )
);
