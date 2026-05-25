-- =====================================================
-- Migration: linha_demonstrativo + RLS + RPCs
-- =====================================================

-- Tabela principal -------------------------------------
create table if not exists public.linha_demonstrativo (
    id_linha       bigint generated always as identity primary key,
    id_empresa     integer not null references public.empresa(id_empresa) on delete cascade,
    tipo           text    not null check (tipo in ('DRE','BP','FLUXO_CAIXA','OUTRO')),
    codigo_conta   text,
    descricao      text    not null,
    periodo        date    not null,
    granularidade  text    not null default 'MENSAL'
                          check (granularidade in ('MENSAL','TRIMESTRAL','SEMESTRAL','ANUAL')),
    valor          numeric(18,2) not null,
    arquivo_origem text,
    data_upload    timestamptz not null default now()
);

create index if not exists idx_linha_empresa_tipo
    on public.linha_demonstrativo (id_empresa, tipo);

create index if not exists idx_linha_empresa_periodo
    on public.linha_demonstrativo (id_empresa, periodo);

-- RLS ---------------------------------------------------
alter table public.linha_demonstrativo enable row level security;

-- Política: usuário só vê/grava linhas das empresas que ele mesmo possui.
-- A tabela empresa já tem id_usuario (referência para auth.uid()).
drop policy if exists "linha_demo_select_own" on public.linha_demonstrativo;
create policy "linha_demo_select_own"
on public.linha_demonstrativo for select
using (
    exists (
        select 1 from public.empresa e
        where e.id_empresa = linha_demonstrativo.id_empresa
          and e.id_usuario = auth.uid()
    )
);

drop policy if exists "linha_demo_insert_own" on public.linha_demonstrativo;
create policy "linha_demo_insert_own"
on public.linha_demonstrativo for insert
with check (
    exists (
        select 1 from public.empresa e
        where e.id_empresa = linha_demonstrativo.id_empresa
          and e.id_usuario = auth.uid()
    )
);

drop policy if exists "linha_demo_delete_own" on public.linha_demonstrativo;
create policy "linha_demo_delete_own"
on public.linha_demonstrativo for delete
using (
    exists (
        select 1 from public.empresa e
        where e.id_empresa = linha_demonstrativo.id_empresa
          and e.id_usuario = auth.uid()
    )
);

-- Helper: empresa do usuário autenticado
create or replace function public.empresa_do_usuario_autenticado()
returns integer
language sql
stable
security invoker
as $$
    select e.id_empresa
    from public.empresa e
    where e.id_usuario = auth.uid()
    limit 1
$$;

-- =====================================================
-- RPCs de agregação para dashboards
-- =====================================================

-- DRE: KPIs + itens da tabela DRE
create or replace function public.dashboard_dre(p_id_empresa integer default null)
returns jsonb
language plpgsql
stable
security invoker
as $$
declare
    v_id_empresa integer := coalesce(p_id_empresa, public.empresa_do_usuario_autenticado());
    v_resultado  jsonb;
begin
    if v_id_empresa is null then
        return jsonb_build_object('hasData', false);
    end if;

    with base as (
        select codigo_conta, descricao, valor
        from public.linha_demonstrativo
        where id_empresa = v_id_empresa
          and tipo = 'DRE'
    ),
    agregado as (
        select
            coalesce(sum(valor) filter (where codigo_conta like '3.01%' or descricao ilike '%receita bruta%'), 0)               as receita_bruta,
            coalesce(sum(valor) filter (where codigo_conta like '3.02%' or descricao ilike '%imposto%'), 0)                    as impostos,
            coalesce(sum(valor) filter (where codigo_conta like '3.03%' or descricao ilike '%deduç%' or descricao ilike '%deduc%'), 0) as deducoes,
            coalesce(sum(valor) filter (where codigo_conta like '3.04%' or descricao ilike '%receita líquida%' or descricao ilike '%receita liquida%'), 0) as receita_liquida,
            coalesce(sum(valor) filter (where codigo_conta like '3.05%' or descricao ilike '%custo%'), 0)                      as cmv,
            coalesce(sum(valor) filter (where codigo_conta like '3.06%' or descricao ilike '%lucro bruto%'), 0)                as lucro_bruto,
            coalesce(sum(valor) filter (where codigo_conta like '3.07%' or descricao ilike '%despesa operacional%' or descricao ilike '%despesas operacionais%'), 0) as desp_operacionais,
            coalesce(sum(valor) filter (where codigo_conta like '3.40%' or codigo_conta = '3.40' or descricao ilike '%lucro líquido%' or descricao ilike '%lucro liquido%'), 0) as lucro_liquido
        from base
    )
    select jsonb_build_object(
        'hasData',          (select count(*) > 0 from base),
        'kpis', jsonb_build_object(
            'receitaBruta',     a.receita_bruta,
            'despOperacionais', abs(a.desp_operacionais),
            'lucroLiquido',     a.lucro_liquido,
            'margemLucro',      case when a.receita_bruta = 0 then 0
                                     else round((a.lucro_liquido / a.receita_bruta) * 100, 2) end
        ),
        'itens', jsonb_build_array(
            jsonb_build_object('label','Receita Bruta',                   'valor', a.receita_bruta,            'bold', true),
            jsonb_build_object('label','(-) Deduções e Impostos',         'valor', -(abs(a.impostos)+abs(a.deducoes)), 'sub',  true),
            jsonb_build_object('label','Receita Líquida',                 'valor', a.receita_liquida,          'bold', true),
            jsonb_build_object('label','(-) Custo das Mercadorias (CMV)', 'valor', -abs(a.cmv),                'sub',  true),
            jsonb_build_object('label','Lucro Bruto',                     'valor', a.lucro_bruto,              'blue', true),
            jsonb_build_object('label','(-) Despesas Operacionais',       'valor', -abs(a.desp_operacionais),  'sub',  true),
            jsonb_build_object('label','Lucro Líquido do Exercício',      'valor', a.lucro_liquido,            'total',true)
        )
    )
    into v_resultado
    from agregado a;

    return v_resultado;
end;
$$;

-- Balanço Patrimonial
create or replace function public.dashboard_balanco(p_id_empresa integer default null)
returns jsonb
language plpgsql
stable
security invoker
as $$
declare
    v_id_empresa integer := coalesce(p_id_empresa, public.empresa_do_usuario_autenticado());
    v_resultado  jsonb;
begin
    if v_id_empresa is null then
        return jsonb_build_object('hasData', false);
    end if;

    with base as (
        select codigo_conta, descricao, valor
        from public.linha_demonstrativo
        where id_empresa = v_id_empresa
          and tipo = 'BP'
    ),
    agregado as (
        select
            coalesce(sum(valor) filter (where codigo_conta like '1%' or descricao ilike '%ativo%'), 0)                       as total_ativo,
            coalesce(sum(valor) filter (where codigo_conta like '1.01%' or descricao ilike '%ativo circulante%'), 0)         as ativo_circulante,
            coalesce(sum(valor) filter (where codigo_conta like '1.02%' or descricao ilike '%realizável%' or descricao ilike '%realizavel%'), 0) as realizavel_lp,
            coalesce(sum(valor) filter (where codigo_conta like '1.03%' or descricao ilike '%permanente%' or descricao ilike '%imobilizado%'), 0) as ativo_permanente,
            coalesce(sum(valor) filter (where codigo_conta like '2.01%' or descricao ilike '%passivo circulante%'), 0)       as passivo_circulante,
            coalesce(sum(valor) filter (where codigo_conta like '2.02%' or descricao ilike '%exigível%' or descricao ilike '%exigivel%'), 0) as exigivel_lp,
            coalesce(sum(valor) filter (where codigo_conta like '2.03%' or descricao ilike '%patrimônio líquido%' or descricao ilike '%patrimonio liquido%'), 0) as patrimonio_liquido
        from base
    )
    select jsonb_build_object(
        'hasData', (select count(*) > 0 from base),
        'kpis', jsonb_build_object(
            'totalAtivo',        a.total_ativo,
            'totalPassivo',      abs(a.passivo_circulante) + abs(a.exigivel_lp),
            'patrimonioLiquido', a.patrimonio_liquido,
            'liquidez',          case when a.passivo_circulante = 0 then null
                                      else round(a.ativo_circulante / abs(a.passivo_circulante), 2) end
        ),
        'ativos', jsonb_build_array(
            jsonb_build_object('nome','Circulante',   'desc','Disponibilidades e CP',     'valor', a.ativo_circulante),
            jsonb_build_object('nome','Realizável LP','desc','Aplicações e Direitos',     'valor', a.realizavel_lp),
            jsonb_build_object('nome','Permanente',   'desc','Imobilizado e Intangível',  'valor', a.ativo_permanente)
        ),
        'recursos', jsonb_build_array(
            jsonb_build_object('nome','Passivo Circulante', 'desc','Obrigações CP',     'valor', abs(a.passivo_circulante)),
            jsonb_build_object('nome','Exigível LP',        'desc','Dívidas Longo Prazo','valor', abs(a.exigivel_lp)),
            jsonb_build_object('nome','Patrimônio Líquido', 'desc','Capital Próprio',   'valor', a.patrimonio_liquido)
        )
    )
    into v_resultado
    from agregado a;

    return v_resultado;
end;
$$;

-- Fluxo de Caixa
create or replace function public.dashboard_fluxo_caixa(p_id_empresa integer default null)
returns jsonb
language plpgsql
stable
security invoker
as $$
declare
    v_id_empresa integer := coalesce(p_id_empresa, public.empresa_do_usuario_autenticado());
    v_resultado  jsonb;
begin
    if v_id_empresa is null then
        return jsonb_build_object('hasData', false);
    end if;

    with base as (
        select descricao, valor, periodo
        from public.linha_demonstrativo
        where id_empresa = v_id_empresa
          and tipo = 'FLUXO_CAIXA'
    ),
    agregado as (
        select
            coalesce(sum(valor) filter (where valor > 0), 0) as entradas,
            coalesce(sum(valor) filter (where valor < 0), 0) as saidas,
            coalesce(sum(valor), 0)                          as saldo
        from base
    )
    select jsonb_build_object(
        'hasData', (select count(*) > 0 from base),
        'kpis', jsonb_build_object(
            'saldoAtual', a.saldo,
            'entradas',   a.entradas,
            'saidas',     abs(a.saidas),
            'projecao30dias', a.saldo + (a.entradas + a.saidas) -- simples
        ),
        'movimentacoes', coalesce(
            (select jsonb_agg(jsonb_build_object(
                    'descricao', descricao,
                    'valor', valor,
                    'periodo', to_char(periodo,'YYYY-MM-DD'),
                    'entrada', (valor >= 0)
                ) order by periodo desc)
             from base
             limit 20), '[]'::jsonb)
    )
    into v_resultado
    from agregado a;

    return v_resultado;
end;
$$;

-- Visão Geral (combina DRE + BP)
create or replace function public.dashboard_visao_geral(p_id_empresa integer default null)
returns jsonb
language plpgsql
stable
security invoker
as $$
declare
    v_id_empresa integer := coalesce(p_id_empresa, public.empresa_do_usuario_autenticado());
    v_resultado  jsonb;
    v_dre        jsonb;
    v_bp         jsonb;
    v_evolucao   jsonb;
begin
    if v_id_empresa is null then
        return jsonb_build_object('hasData', false);
    end if;

    v_dre := public.dashboard_dre(v_id_empresa);
    v_bp  := public.dashboard_balanco(v_id_empresa);

    -- Evolução mensal de receita e despesa (últimos 6 meses)
    select coalesce(jsonb_agg(t.row order by t.periodo), '[]'::jsonb)
    into v_evolucao
    from (
        select periodo,
               jsonb_build_object(
                   'mes', to_char(periodo, 'Mon'),
                   'receita', coalesce(sum(valor) filter (where descricao ilike '%receita%'), 0),
                   'despesa', coalesce(sum(valor) filter (where descricao ilike '%despesa%' or descricao ilike '%custo%'), 0)
               ) as row
        from public.linha_demonstrativo
        where id_empresa = v_id_empresa
          and tipo = 'DRE'
        group by periodo
        order by periodo desc
        limit 6
    ) t;

    v_resultado := jsonb_build_object(
        'hasData', (v_dre->>'hasData')::boolean or (v_bp->>'hasData')::boolean,
        'kpis', jsonb_build_object(
            'saldoAtual',        coalesce(v_dre->'kpis'->>'lucroLiquido','0')::numeric,
            'lucroLiquido',      coalesce(v_dre->'kpis'->>'lucroLiquido','0')::numeric,
            'margem',            coalesce(v_dre->'kpis'->>'margemLucro','0')::numeric,
            'patrimonioLiquido', coalesce(v_bp ->'kpis'->>'patrimonioLiquido','0')::numeric,
            'liquidez',          coalesce(v_bp ->'kpis'->>'liquidez','0')::numeric
        ),
        'evolucao', v_evolucao
    );

    return v_resultado;
end;
$$;

-- =====================================================
-- RPC para o chat: resumo textual do estado financeiro
-- Internamente chama as 4 RPCs de dashboard (dre, balanco,
-- fluxo_caixa, visao_geral) e formata os mesmos KPIs
-- exibidos nas telas em texto natural para o LLM.
-- =====================================================
create or replace function public.resumo_financeiro_chat(p_id_empresa integer default null)
returns text
language plpgsql
stable
security invoker
as $$
declare
    v_id_empresa integer := coalesce(p_id_empresa, public.empresa_do_usuario_autenticado());
    v_dre        jsonb;
    v_bp         jsonb;
    v_fluxo      jsonb;
    v_visao      jsonb;
    v_buf        text := '';
begin
    if v_id_empresa is null then
        return 'Sem empresa vinculada ao usuario autenticado. Oriente-o a concluir o cadastro.';
    end if;

    v_visao := public.dashboard_visao_geral(v_id_empresa);

    if not coalesce((v_visao->>'hasData')::boolean, false) then
        return E'CONTEXTO FINANCEIRO: nenhuma planilha importada ainda.\n'
            || 'Oriente o usuario a fazer upload em /upload-planilha para que voce '
            || 'possa analisar os dados reais da empresa dele.';
    end if;

    v_dre   := public.dashboard_dre(v_id_empresa);
    v_bp    := public.dashboard_balanco(v_id_empresa);
    v_fluxo := public.dashboard_fluxo_caixa(v_id_empresa);

    v_buf := E'CONTEXTO FINANCEIRO ATUAL DA EMPRESA (use como referencia factual):\n';

    -- DRE
    if coalesce((v_dre->>'hasData')::boolean, false) then
        v_buf := v_buf || format(
            E'\n[DRE]\n  Receita Bruta: R$ %s\n  Despesas Operacionais: R$ %s\n  Lucro Liquido: R$ %s\n  Margem de Lucro: %s%%\n',
            to_char((v_dre->'kpis'->>'receitaBruta')::numeric,     'FM999G999G999D00'),
            to_char((v_dre->'kpis'->>'despOperacionais')::numeric, 'FM999G999G999D00'),
            to_char((v_dre->'kpis'->>'lucroLiquido')::numeric,     'FM999G999G999D00'),
            to_char((v_dre->'kpis'->>'margemLucro')::numeric,      'FM990D00')
        );
    end if;

    -- Balanço Patrimonial
    if coalesce((v_bp->>'hasData')::boolean, false) then
        v_buf := v_buf || format(
            E'\n[BALANCO PATRIMONIAL]\n  Total Ativo: R$ %s\n  Total Passivo: R$ %s\n  Patrimonio Liquido: R$ %s\n  Indice de Liquidez: %s\n',
            to_char((v_bp->'kpis'->>'totalAtivo')::numeric,        'FM999G999G999D00'),
            to_char((v_bp->'kpis'->>'totalPassivo')::numeric,      'FM999G999G999D00'),
            to_char((v_bp->'kpis'->>'patrimonioLiquido')::numeric, 'FM999G999G999D00'),
            coalesce(
                to_char(nullif(v_bp->'kpis'->>'liquidez','')::numeric, 'FM990D00'),
                'n/d'
            )
        );
    end if;

    -- Fluxo de Caixa
    if coalesce((v_fluxo->>'hasData')::boolean, false) then
        v_buf := v_buf || format(
            E'\n[FLUXO DE CAIXA]\n  Saldo Atual: R$ %s\n  Entradas: R$ %s\n  Saidas: R$ %s\n  Projecao 30 dias: R$ %s\n',
            to_char((v_fluxo->'kpis'->>'saldoAtual')::numeric,     'FM999G999G999D00'),
            to_char((v_fluxo->'kpis'->>'entradas')::numeric,       'FM999G999G999D00'),
            to_char((v_fluxo->'kpis'->>'saidas')::numeric,         'FM999G999G999D00'),
            to_char((v_fluxo->'kpis'->>'projecao30dias')::numeric, 'FM999G999G999D00')
        );
    end if;

    return v_buf || E'\nResponda sempre baseado nestes numeros; eles refletem o que esta nos dashboards do usuario.';
end;
$$;

grant execute on function public.dashboard_dre(integer)            to authenticated;
grant execute on function public.dashboard_balanco(integer)        to authenticated;
grant execute on function public.dashboard_fluxo_caixa(integer)    to authenticated;
grant execute on function public.dashboard_visao_geral(integer)    to authenticated;
grant execute on function public.resumo_financeiro_chat(integer)   to authenticated;
grant execute on function public.empresa_do_usuario_autenticado()  to authenticated;
