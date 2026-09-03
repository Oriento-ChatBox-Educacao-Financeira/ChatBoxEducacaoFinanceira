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
    const userId = 'f8d61eef-28cb-409c-a368-d326534c5341';

    const exists = await client.query(
      `select id_usuario, nome, email from public.usuario where id_usuario = $1`,
      [userId]
    );
    console.log('usuario na public.usuario?', exists.rowCount > 0 ? exists.rows[0] : 'NAO');

    if (exists.rowCount === 0) {
      console.log('SKIP: usuario nao existe, nao da pra testar INSERT.');
      return;
    }

    const ins = await client.query(
      `insert into public.notificacao (id_usuario, tipo, titulo, mensagem)
       values ($1, 'GERAL', 'fk-smoke', 'verificacao apos fix de FK')
       returning id_notificacao`,
      [userId]
    );
    console.log('INSERT OK, id_notificacao =', ins.rows[0].id_notificacao);

    await client.query(
      `delete from public.notificacao where id_notificacao = $1`,
      [ins.rows[0].id_notificacao]
    );
    console.log('limpou registro de teste.');
  } catch (e) {
    console.error('ERR', e.message);
    process.exitCode = 1;
  } finally {
    await client.end();
  }
})();
