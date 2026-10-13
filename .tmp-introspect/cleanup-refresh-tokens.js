/**
 * Limpa refresh tokens duplicados por id_usuario. Mant\u00e9m apenas o
 * mais recente (maior id) por usu\u00e1rio. Necess\u00e1rio porque uma vers\u00e3o
 * anterior do c\u00f3digo permitia que m\u00faltiplos tokens convivessem na
 * tabela, quebrando o {@code findByUsuario} (Optional) no login subsequente.
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

(async () => {
  try {
    await client.connect();

    console.log('--- ANTES ---');
    const before = await client.query(`
      select id_usuario, count(*) as qtd
      from public.refresh_token
      group by id_usuario having count(*) > 1
      order by qtd desc
    `);
    before.rows.forEach((r) => console.log(' ', r));

    const del = await client.query(`
      delete from public.refresh_token
      where id not in (
        select max(id) from public.refresh_token group by id_usuario
      )
      returning id, id_usuario
    `);
    console.log(`\n--- DELETADOS (${del.rowCount}) ---`);
    del.rows.forEach((r) => console.log(' ', r));

    console.log('\n--- DEPOIS ---');
    const after = await client.query(`
      select id_usuario, count(*) as qtd
      from public.refresh_token
      group by id_usuario having count(*) > 1
    `);
    console.log(' ', after.rowCount === 0 ? 'OK, sem duplicatas.' : after.rows);
  } catch (e) {
    console.error('ERR', e.message);
    process.exitCode = 1;
  } finally {
    await client.end();
  }
})();
