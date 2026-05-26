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

    const tables = ['refresh_token', 'conversa'];
    for (const t of tables) {
      const cols = await client.query(
        `select column_name, data_type, udt_name, is_nullable, column_default, character_maximum_length
         from information_schema.columns
         where table_schema='public' and table_name=$1
         order by ordinal_position`,
        [t]
      );
      if (cols.rowCount === 0) {
        console.log(`\n[${t}] (table not found in public)`);
      } else {
        console.log(`\n[${t}]`);
        cols.rows.forEach((c) => console.log(' ', c));
      }
    }

    // List all enum types
    const enums = await client.query(
      `select tp.typname, e.enumlabel
       from pg_type tp join pg_enum e on e.enumtypid = tp.oid
       where tp.typnamespace = (select oid from pg_namespace where nspname='public')
       order by tp.typname, e.enumsortorder`
    );
    console.log('\n[all public enums]');
    const grouped = {};
    enums.rows.forEach((r) => {
      grouped[r.typname] = grouped[r.typname] ?? [];
      grouped[r.typname].push(r.enumlabel);
    });
    Object.entries(grouped).forEach(([k, v]) => console.log(' ', k, '=>', v));

    // List all public tables to ensure no others are missing
    const allTables = await client.query(
      `select table_name from information_schema.tables
       where table_schema='public' and table_type='BASE TABLE'
       order by table_name`
    );
    console.log('\n[all public tables]');
    allTables.rows.forEach((r) => console.log(' ', r.table_name));
  } catch (e) {
    console.error('ERR', e.message);
  } finally {
    await client.end();
  }
})();
