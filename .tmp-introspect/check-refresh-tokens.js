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

    console.log('--- colunas de refresh_token ---');
    const cols = await client.query(`
      select column_name, data_type, is_nullable
      from information_schema.columns
      where table_schema='public' and table_name='refresh_token'
      order by ordinal_position
    `);
    cols.rows.forEach((r) => console.log(' ', r));

    console.log('\n--- top usuarios com >1 refresh_token ---');
    const dup = await client.query(`
      select id_usuario, count(*) as qtd
      from public.refresh_token
      group by id_usuario having count(*) > 1
      order by qtd desc
      limit 10
    `);
    dup.rows.forEach((r) => console.log(' ', r));

    console.log('\n--- refresh_tokens do f8d61eef-... ---');
    const r = await client.query(
      `select * from public.refresh_token where id_usuario = $1`,
      ['f8d61eef-28cb-409c-a368-d326534c5341']
    );
    r.rows.forEach((row) => console.log(' ', row));
  } catch (e) {
    console.error('ERR', e.message);
    process.exitCode = 1;
  } finally {
    await client.end();
  }
})();
