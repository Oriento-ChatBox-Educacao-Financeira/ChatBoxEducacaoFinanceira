/**
 * Migrac\u0327\u00e3o pontual: a FK `notificacao_id_usuario_fkey` referencia
 * `auth.users(id)` (legado do Supabase Auth). Apo\u0301s migrar para JWT pro\u0301prio,
 * os usua\u0301rios sa\u0303o criados em `public.usuario` e na\u0303o em `auth.users`,
 * causando viola\u0327\u00e3o de FK em qualquer INSERT em `notificacao`.
 *
 * Este script:
 *   1. Lista as FKs atuais de public.notificacao e public.usuario.
 *   2. Dropa FK `notificacao_id_usuario_fkey` e recria apontando para
 *      `public.usuario(id_usuario)` ON DELETE CASCADE.
 *   3. Tambe\u0301m corrige a FK de public.usuario.id_usuario se ela
 *      apontar para auth.users (na\u0303o precisamos mais dessa depende\u0302ncia).
 */
const { Client } = require('pg');

const client = new Client({
  host: 'aws-1-us-east-1.pooler.supabase.com',
  port: 5432,
  database: 'postgres',
  user: 'postgres.gchjfmvajatdbefjycjh',
  password: 'Chat@Oriento#Financeiro26',
  ssl: { rejectUnauthorized: false },
});

const FK_QUERY = `
  select c.conname,
         n2.nspname || '.' || cl2.relname as referenced_table,
         pg_get_constraintdef(c.oid)      as definition
  from pg_constraint c
  join pg_class    cl  on cl.oid = c.conrelid
  join pg_namespace n   on n.oid = cl.relnamespace
  join pg_class    cl2 on cl2.oid = c.confrelid
  join pg_namespace n2  on n2.oid = cl2.relnamespace
  where c.contype = 'f'
    and n.nspname = 'public'
    and cl.relname = $1
`;

(async () => {
  try {
    await client.connect();

    console.log('--- FKs ANTES ---');
    for (const t of ['notificacao', 'usuario']) {
      const r = await client.query(FK_QUERY, [t]);
      console.log(`\n[public.${t}]`);
      r.rows.forEach((row) => console.log(' ', row.conname, '->', row.referenced_table, '|', row.definition));
    }

    console.log('\n--- Aplicando correc\u0327\u00f5es ---');

    // Notificacao -> public.usuario
    await client.query(`alter table public.notificacao drop constraint if exists notificacao_id_usuario_fkey`);
    await client.query(`
      alter table public.notificacao
        add constraint notificacao_id_usuario_fkey
        foreign key (id_usuario) references public.usuario(id_usuario) on delete cascade
    `);
    console.log('  notificacao_id_usuario_fkey -> public.usuario(id_usuario) [OK]');

    // Usuario.id_usuario era FK para auth.users(id) no schema base; agora ele
    // \u00e9 gerado/gerenciado pelo backend Java. Removemos a depende\u0302ncia
    // se ela ainda existir.
    const usuarioFks = await client.query(FK_QUERY, ['usuario']);
    for (const row of usuarioFks.rows) {
      if (row.referenced_table === 'auth.users') {
        await client.query(`alter table public.usuario drop constraint if exists ${row.conname}`);
        console.log(`  usuario.${row.conname} (-> auth.users) DROPPED`);
      }
    }

    console.log('\n--- FKs DEPOIS ---');
    for (const t of ['notificacao', 'usuario']) {
      const r = await client.query(FK_QUERY, [t]);
      console.log(`\n[public.${t}]`);
      r.rows.forEach((row) => console.log(' ', row.conname, '->', row.referenced_table, '|', row.definition));
    }
  } catch (e) {
    console.error('ERR', e.message);
    process.exitCode = 1;
  } finally {
    await client.end();
  }
})();
