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

    console.log('--- usuarios duplicados por email ---');
    const dupUsers = await client.query(`
      select email, count(*) as qtd
      from public.usuario
      group by email having count(*) > 1
      order by qtd desc
    `);
    dupUsers.rows.forEach((r) => console.log(' ', r));

    console.log('\n--- empresas por id_usuario (top 10 com >1) ---');
    const dupEmps = await client.query(`
      select id_usuario, count(*) as qtd
      from public.empresa
      where id_usuario is not null
      group by id_usuario having count(*) > 1
      order by qtd desc
      limit 10
    `);
    dupEmps.rows.forEach((r) => console.log(' ', r));

    console.log('\n--- usuario f8d61eef-... ---');
    const u = await client.query(
      `select id_usuario, nome, email from public.usuario where id_usuario = $1`,
      ['f8d61eef-28cb-409c-a368-d326534c5341']
    );
    u.rows.forEach((r) => console.log(' ', r));

    console.log('\n--- empresas desse usuario ---');
    const e = await client.query(
      `select id_empresa, cnpj, nome_fantasia, regime_tributario, data_criacao
       from public.empresa
       where id_usuario = $1
       order by data_criacao`,
      ['f8d61eef-28cb-409c-a368-d326534c5341']
    );
    e.rows.forEach((r) => console.log(' ', r));
  } catch (e) {
    console.error('ERR', e.message);
    process.exitCode = 1;
  } finally {
    await client.end();
  }
})();
