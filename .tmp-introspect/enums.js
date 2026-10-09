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
    const types = ['porte_empresa', 'regime_tributario_tipo', 'porte', 'regimetributario', 'nivelmaturidadefinanceira'];
    for (const t of types) {
      const r = await client.query(
        `select e.enumlabel
         from pg_type tp
         join pg_enum e on e.enumtypid = tp.oid
         where tp.typname = $1
         order by e.enumsortorder`,
        [t]
      );
      console.log(`\n[${t}] (${r.rowCount})`);
      r.rows.forEach((row) => console.log('  -', JSON.stringify(row.enumlabel)));
    }

    const emp = await client.query(
      `select column_name, udt_name, data_type, is_nullable
       from information_schema.columns
       where table_schema='public' and table_name='empresa'
       order by ordinal_position`
    );
    console.log('\n[empresa columns]');
    emp.rows.forEach((row) => console.log(' ', row));
  } catch (e) {
    console.error('ERR', e.message);
  } finally {
    await client.end();
  }
})();
