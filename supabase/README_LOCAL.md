# Teste local — pipeline completo (sem tocar no Supabase remoto)

## Pré-requisitos

- **Docker Desktop** instalado e rodando
- **Node 20+** e npm
- **Supabase CLI**: `npm i -g supabase`

## 1) Subir o stack Supabase local

Na raiz do repositório:

```bash
supabase start
```

O CLI sobe Postgres, Auth, Storage, Studio e Edge Functions. Aplica em ordem
as migrations em `supabase/migrations/`:

1. `20260523_local_baseline.sql` — cria `usuario`, `empresa`, enums, RLS e a trigger `handle_new_user`
2. `20260525_linha_demonstrativo.sql` — tabela `linha_demonstrativo`, RLS, RPCs dos dashboards
3. `20260525_storage_planilhas.sql` — bucket `planilhas` + policies

Anote do output:
```
API URL:    http://127.0.0.1:54321
Studio URL: http://127.0.0.1:54323
anon key:   eyJhbGc...
```

## 2) Servir a Edge Function

Em outro terminal:

```bash
supabase functions serve parse-planilha
```

Disponível em `http://127.0.0.1:54321/functions/v1/parse-planilha`. Hot reload ativo.

## 3) Apontar o frontend para o Supabase local

**Editar `frontend/src/environments/environment.ts`** (não commitar):

```ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  authUrl: 'http://localhost:8080/api/auth',
  geminiUrl: 'http://localhost:8080/api/oriento/ask',
  supabase: {
    url: 'http://127.0.0.1:54321',
    anonKey: '<cole a anon key do supabase start>',
  },
};
```

Subir o front:

```bash
cd frontend
npm install
npm start         # http://localhost:4200
```

## 4) Fluxo de teste

### Cadastrar e logar

1. Acesse `http://localhost:4200/register`
2. Preencha nome, e-mail, senha, nome fantasia, CNPJ (qualquer)
3. Submeta → a trigger `handle_new_user` popula `usuario` e `empresa` no Postgres local
4. Faça login

> Auth está 100% no Supabase local — não existe e-mail de confirmação, login funciona direto.

### Importar planilha

1. Acesse `/upload-planilha`
2. Clique em **Baixar template CSV**
3. Preencha (exemplo):
   ```
   tipo;codigo;descricao;periodo;valor;granularidade
   DRE;3.01.01;Receita com Mercadorias e Produtos;2025-01;125000,00;MENSAL
   DRE;3.05.01;Custos das Mercadorias;2025-01;-48000,00;MENSAL
   DRE;3.07;Despesas Operacionais;2025-01;-22000,00;MENSAL
   BP;1.01;Ativo Circulante;2025-01;82000,00;MENSAL
   BP;2.01;Passivo Circulante;2025-01;-30000,00;MENSAL
   BP;2.03;Patrimônio Líquido;2025-01;52000,00;MENSAL
   FLUXO_CAIXA;;Recebimento Cliente A;2025-01;15000,00;MENSAL
   FLUXO_CAIXA;;Pagamento Fornecedor;2025-01;-4500,00;MENSAL
   ```
4. Upload → mensagem "Importação concluída"

Confirme no Studio (`http://127.0.0.1:54323`):
- **Storage → planilhas → `<seu_user_id>/`** tem o arquivo
- **public.linha_demonstrativo** tem as linhas

### Validar dashboards

Acesse cada uma:
- `/dashboard/visao-geral`
- `/dashboard/dre`
- `/dashboard/balanco-patrimonial`
- `/dashboard/fluxo-caixa`

Sem dados → banner amarelo com link para upload.
Com dados → KPIs e tabelas populadas a partir da planilha.

### Validar RPCs direto (opcional)

No Studio → SQL editor:

```sql
select public.dashboard_dre();
select public.dashboard_balanco();
select public.dashboard_fluxo_caixa();
select public.dashboard_visao_geral();
select public.resumo_financeiro_chat();
```

Os RPCs usam `auth.uid()` — no SQL editor do Studio você precisa logar como
o usuário (ícone "Run as authenticated user") ou passar `p_id_empresa`
explicitamente.

## 5) Reset

```bash
supabase db reset       # recria do zero e reaplica todas as migrations
supabase stop --no-backup
```

## Limitações conhecidas no local

- **CNPJ duplicado**: a trigger soma `extract(epoch from now())` ao CNPJ se já existir, só para não quebrar o local. Em produção a unicidade real é mantida.
- **`nivel_maturidade`** entra como `INICIANTE` por default — o formulário de cadastro não pergunta isso.
- **Backend Java** continua intacto — login/cadastro não passam por ele, mas o chat `/api/oriento/ask` sim. Se quiser testar o chat com contexto financeiro, precisaria mover ele para Edge Function (não fizemos).
- **Parser de planilha** aceita o formato canônico (CSV/XLSX com colunas `tipo;codigo;descricao;periodo;valor;granularidade`). Os arquivos `Dados Input - Empresa N.xlsx` têm layout cruzado (períodos como colunas) — não funcionam nesse parser.

## Estrutura

```
supabase/
├── config.toml                              # config do CLI
├── seed.sql                                 # diagnóstico opcional
├── README_LOCAL.md                          # este arquivo
├── migrations/
│   ├── 20260523_local_baseline.sql          # usuario+empresa+trigger
│   ├── 20260525_linha_demonstrativo.sql     # tabela+RLS+RPCs
│   └── 20260525_storage_planilhas.sql       # bucket+policies
└── functions/
    └── parse-planilha/index.ts              # Edge Function Deno
```
