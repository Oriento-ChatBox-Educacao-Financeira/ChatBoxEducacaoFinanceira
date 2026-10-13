// Edge Function: parse-planilha
// Recebe { path, idEmpresa? } no body, baixa o arquivo do bucket 'planilhas',
// parseia (CSV/XLSX) e insere em public.linha_demonstrativo.
// Usa o JWT do usuário chamador — RLS garante isolamento.
//
// Deploy:
//   supabase functions deploy parse-planilha --no-verify-jwt=false

// deno-lint-ignore-file no-explicit-any
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";
import { read, utils } from "https://esm.sh/xlsx@0.18.5";
import { parse as parseCsv } from "https://deno.land/std@0.224.0/csv/parse.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

interface ReqBody {
  path: string;
  idEmpresa?: number;
  hashArquivo?: string;
}

interface LinhaParsed {
  tipo: "DRE" | "BP" | "FLUXO_CAIXA" | "OUTRO";
  codigo_conta: string | null;
  descricao: string;
  periodo: string; // YYYY-MM-DD
  granularidade: "MENSAL" | "TRIMESTRAL" | "SEMESTRAL" | "ANUAL";
  valor: number;
}

function normalize(s: string): string {
  return (s ?? "")
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .trim()
    .toLowerCase();
}

function parseTipo(raw: string): LinhaParsed["tipo"] {
  const n = normalize(raw);
  if (n === "dre") return "DRE";
  if (["bp", "balanco", "balanco patrimonial"].includes(n)) return "BP";
  if (["fluxo", "fluxo caixa", "fluxo_caixa", "dfc"].includes(n)) return "FLUXO_CAIXA";
  return "OUTRO";
}

function parseGranularidade(raw: string | undefined): LinhaParsed["granularidade"] {
  const n = normalize(raw ?? "");
  if (n.startsWith("tri")) return "TRIMESTRAL";
  if (n.startsWith("sem")) return "SEMESTRAL";
  if (n.startsWith("ano") || n === "anual") return "ANUAL";
  return "MENSAL";
}

function parseValor(raw: any): number {
  if (typeof raw === "number") return raw;
  let s = String(raw ?? "").trim();
  let neg = false;
  if (s.startsWith("(") && s.endsWith(")")) { neg = true; s = s.slice(1, -1); }
  s = s.replace(/R\$/g, "").replace(/\s/g, "");
  if (s.includes(",") && s.includes(".")) s = s.replace(/\./g, "").replace(",", ".");
  else if (s.includes(",")) s = s.replace(",", ".");
  const v = Number(s);
  if (!Number.isFinite(v)) throw new Error(`Valor inválido: ${raw}`);
  return neg ? -v : v;
}

function parsePeriodo(raw: any): string {
  // Excel serial
  if (typeof raw === "number" && raw > 25569 && raw < 73050) {
    const ms = (raw - 25569) * 86400 * 1000;
    return new Date(ms).toISOString().slice(0, 10).replace(/-\d{2}$/, "-01");
  }
  const s = String(raw ?? "").trim();
  // YYYY-MM-DD
  if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return s.slice(0, 7) + "-01";
  // YYYY-MM
  if (/^\d{4}-\d{2}$/.test(s)) return s + "-01";
  // DD/MM/YYYY
  let m = s.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
  if (m) return `${m[3]}-${m[2]}-01`;
  // MM/YYYY
  m = s.match(/^(\d{2})\/(\d{4})$/);
  if (m) return `${m[2]}-${m[1]}-01`;
  throw new Error(`Período inválido: ${raw}`);
}

function montarLinhaFlat(row: Record<string, any>): LinhaParsed | null {
  const keys: Record<string, string> = {};
  for (const k of Object.keys(row)) keys[normalize(k)] = k;
  const get = (n: string) => row[keys[n]];

  const descricao = String(get("descricao") ?? "").trim();
  const periodoRaw = get("periodo");
  const valorRaw = get("valor");
  if (!descricao || periodoRaw == null || valorRaw == null || valorRaw === "") return null;

  try {
    return {
      tipo: parseTipo(String(get("tipo") ?? "")),
      codigo_conta: get("codigo") ? String(get("codigo")).trim() : null,
      descricao,
      periodo: parsePeriodo(periodoRaw),
      granularidade: parseGranularidade(get("granularidade")),
      valor: parseValor(valorRaw),
    };
  } catch (_e) {
    return null;
  }
}

/**
 * Detecta o tipo a partir do texto do marcador (1ª coluna ou 1ª linha):
 *   "DRE"           → DRE
 *   "BP", "Balanço" → BP
 *   "Fluxo*"        → FLUXO_CAIXA
 *   "Capital*"      → BP (capital de giro é parte do BP)
 */
function detectMarker(cell: any): LinhaParsed["tipo"] | null {
  const n = normalize(String(cell ?? ""));
  if (!n) return null;
  if (n === "dre") return "DRE";
  if (n === "bp" || n.startsWith("balanco")) return "BP";
  if (n.startsWith("fluxo") || n === "dfc") return "FLUXO_CAIXA";
  if (n.startsWith("capital")) return "BP";
  return null;
}

/**
 * Tenta interpretar uma célula como data de período (mês). Retorna YYYY-MM-01
 * ou null se não for data válida.
 */
function tryPeriodo(cell: any): string | null {
  if (cell == null || cell === "") return null;
  try {
    const p = parsePeriodo(cell);
    return p;
  } catch {
    return null;
  }
}

/**
 * Formato WIDE: para cada aba, varre as linhas procurando marcadores
 * (DRE / BP / Fluxo / Capital). Quando acha um marcador, lê os meses na
 * mesma linha (colunas C em diante) e depois as próximas linhas como contas
 * (col A = código, col B = descrição, cols C..N = valores por mês).
 * Para quando: acha outro marcador, ou várias linhas vazias seguidas.
 */
function parseSheetWide(sheet: any): LinhaParsed[] {
  const matrix: any[][] = utils.sheet_to_json(sheet, { header: 1, defval: null }) as any[][];
  const out: LinhaParsed[] = [];

  let tipo: LinhaParsed["tipo"] | null = null;
  let meses: (string | null)[] = [];
  let vaziasConsecutivas = 0;

  for (let r = 0; r < matrix.length; r++) {
    const row = matrix[r] ?? [];
    const a = row[0];
    const b = row[1];

    // Marcador na coluna A ou B?
    const marker = detectMarker(a) ?? detectMarker(b);
    if (marker) {
      tipo = marker;
      // Tenta capturar os meses na mesma linha (col C..)
      meses = row.slice(2).map((c) => tryPeriodo(c));
      // Se nenhum mês foi capturado, tenta a próxima linha
      if (!meses.some((m) => m !== null)) {
        const next = matrix[r + 1] ?? [];
        meses = next.slice(2).map((c) => tryPeriodo(c));
        if (meses.some((m) => m !== null)) r++;
      }
      vaziasConsecutivas = 0;
      continue;
    }

    if (!tipo || !meses.some((m) => m !== null)) continue;

    // Linha vazia?
    const todaVazia = row.every((c) => c == null || String(c).trim() === "");
    if (todaVazia) {
      vaziasConsecutivas++;
      if (vaziasConsecutivas >= 3) {
        tipo = null;
        meses = [];
        vaziasConsecutivas = 0;
      }
      continue;
    }
    vaziasConsecutivas = 0;

    const codigo = a != null && String(a).trim() !== "" ? String(a).trim() : null;
    const descricao = b != null ? String(b).trim() : "";
    if (!descricao) continue;

    // Para cada mês, lê o valor da coluna correspondente
    for (let i = 0; i < meses.length; i++) {
      const periodo = meses[i];
      if (!periodo) continue;
      const raw = row[i + 2];
      if (raw == null || raw === "") continue;
      try {
        const valor = parseValor(raw);
        if (valor === 0) continue; // pula zeros para não inflar a base
        out.push({
          tipo,
          codigo_conta: codigo,
          descricao,
          periodo,
          granularidade: "MENSAL",
          valor,
        });
      } catch {
        // valor inválido, pula
      }
    }
  }

  return out;
}

async function parseFile(buffer: ArrayBuffer, filename: string): Promise<LinhaParsed[]> {
  const lower = filename.toLowerCase();
  if (lower.endsWith(".csv")) {
    const text = new TextDecoder("utf-8").decode(buffer);
    const delimiter = text.split("\n")[0]?.includes(";") ? ";" : ",";
    const rows = parseCsv(text, { skipFirstRow: true, separator: delimiter });
    return rows.map((r: any) => montarLinhaFlat(r)).filter((x): x is LinhaParsed => !!x);
  }
  if (lower.endsWith(".xlsx") || lower.endsWith(".xlsm")) {
    const wb = read(new Uint8Array(buffer), { type: "array", cellDates: false });
    const all: LinhaParsed[] = [];

    for (const sheetName of wb.SheetNames) {
      const sheet = wb.Sheets[sheetName];

      // Estratégia 1: tenta formato FLAT (1ª linha = headers tipo/codigo/descricao/periodo/valor)
      const flatRows = utils.sheet_to_json<Record<string, any>>(sheet, { defval: null });
      const fromFlat = flatRows
        .map((r) => montarLinhaFlat(r))
        .filter((x): x is LinhaParsed => !!x);
      if (fromFlat.length > 0) {
        all.push(...fromFlat);
        continue;
      }

      // Estratégia 2: formato WIDE com marcadores DRE/BP/Fluxo/Capital
      all.push(...parseSheetWide(sheet));
    }
    return all;
  }
  throw new Error("Formato não suportado. Use .csv ou .xlsx");
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }
  try {
    const auth = req.headers.get("Authorization");
    if (!auth) {
      return new Response(JSON.stringify({ error: "missing Authorization" }), {
        status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_ANON_KEY")!,
      { global: { headers: { Authorization: auth } } }
    );

    const body = (await req.json()) as ReqBody;
    if (!body?.path) {
      return new Response(JSON.stringify({ error: "path obrigatório" }), {
        status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // Descobre id_empresa do usuário se não foi informado
    let idEmpresa = body.idEmpresa;
    if (!idEmpresa) {
      const { data, error } = await supabase
        .from("empresa")
        .select("id_empresa")
        .limit(1)
        .maybeSingle();
      if (error) throw error;
      if (!data) {
        return new Response(JSON.stringify({ error: "Empresa não vinculada ao usuário" }), {
          status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }
      idEmpresa = data.id_empresa;
    }

    // Baixa o arquivo do Storage
    const { data: file, error: dlErr } = await supabase.storage
      .from("planilhas")
      .download(body.path);
    if (dlErr || !file) {
      return new Response(JSON.stringify({ error: `download falhou: ${dlErr?.message}` }), {
        status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const buffer = await file.arrayBuffer();
    const linhas = await parseFile(buffer, body.path);

    if (linhas.length === 0) {
      return new Response(JSON.stringify({ error: "Nenhuma linha válida encontrada" }), {
        status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const arquivoOrigem = body.path.split("/").pop() ?? body.path;

    // Checagem de duplicata por hash (server-side, defesa em profundidade)
    if (body.hashArquivo) {
      const { data: dup, error: dupErr } = await supabase
        .from("linha_demonstrativo")
        .select("data_upload, arquivo_origem")
        .eq("id_empresa", idEmpresa)
        .eq("hash_arquivo", body.hashArquivo)
        .limit(1)
        .maybeSingle();
      if (dupErr) throw dupErr;
      if (dup) {
        return new Response(
          JSON.stringify({
            error: "duplicada",
            mensagem: `Esta planilha já foi importada em ${new Date(dup.data_upload).toLocaleString("pt-BR")} como "${dup.arquivo_origem}".`,
            arquivoOriginal: dup.arquivo_origem,
            dataUpload: dup.data_upload,
          }),
          { status: 409, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }
    }

    // Deduplica por chave natural (empresa, tipo, codigo, descricao, periodo)
    // somando valores quando a mesma conta aparece em abas/blocos diferentes.
    // Postgres recusa um upsert com a mesma chave aparecendo 2x no batch.
    const dedup = new Map<string, LinhaParsed>();
    for (const l of linhas) {
      const key = `${l.tipo}|${l.codigo_conta ?? ''}|${l.descricao.toLowerCase()}|${l.periodo}`;
      const ex = dedup.get(key);
      if (ex) {
        ex.valor += l.valor;
      } else {
        dedup.set(key, { ...l });
      }
    }

    const rows = Array.from(dedup.values()).map((l) => ({
      id_empresa: idEmpresa,
      tipo: l.tipo,
      codigo_conta: l.codigo_conta,
      descricao: l.descricao,
      periodo: l.periodo,
      granularidade: l.granularidade,
      valor: l.valor,
      arquivo_origem: arquivoOrigem,
      hash_arquivo: body.hashArquivo ?? null,
    }));

    // UPSERT por chave natural: atualiza valor se (empresa, tipo, código, descrição, período) já existir
    const { error: insErr, count } = await supabase
      .from("linha_demonstrativo")
      .upsert(rows, {
        onConflict: "id_empresa,tipo,codigo_conta,descricao,periodo",
        count: "exact",
      });

    if (insErr) {
      return new Response(JSON.stringify({ error: `upsert falhou: ${insErr.message}` }), {
        status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // Contagem por tipo para detectar planilha incompleta
    const tiposPresentes = new Set(rows.map((r) => r.tipo));
    const tiposEsperados = ["DRE", "BP", "FLUXO_CAIXA"] as const;
    const tiposFaltantes = tiposEsperados.filter((t) => !tiposPresentes.has(t));
    const avisos: string[] = [];
    const map: Record<string, string> = {
      DRE: "DRE",
      BP: "Balanço Patrimonial",
      FLUXO_CAIXA: "Fluxo de Caixa",
    };
    if (tiposFaltantes.length > 0) {
      avisos.push(
        `Planilha incompleta — sem dados para: ${tiposFaltantes.map((t) => map[t]).join(", ")}. Os dashboards correspondentes ficarão zerados.`
      );
    }

    // Cria notificações pro usuário autenticado
    const { data: userData } = await supabase.auth.getUser();
    const userId = userData?.user?.id;
    if (userId) {
      const notifs: Array<Record<string, unknown>> = [
        {
          id_usuario: userId,
          tipo: "PLANILHA_IMPORTADA",
          titulo: "Planilha importada",
          mensagem: `${count ?? rows.length} linhas processadas de "${arquivoOrigem}".`,
          icone: "ph-check-circle",
          link: "/dashboard/visao-geral",
        },
      ];
      if (tiposFaltantes.length > 0) {
        notifs.push({
          id_usuario: userId,
          tipo: "PLANILHA_INCOMPLETA",
          titulo: "Dashboards com dados zerados",
          mensagem: `A planilha não contém dados de ${tiposFaltantes.map((t) => map[t]).join(", ")}. Esses dashboards aparecerão zerados até você importar uma planilha com essas seções.`,
          icone: "ph-warning",
          link: "/upload-planilha",
        });
      }
      await supabase.from("notificacao").insert(notifs);
    }

    return new Response(
      JSON.stringify({
        arquivo: arquivoOrigem,
        idEmpresa,
        linhasImportadas: count ?? rows.length,
        tiposPresentes: Array.from(tiposPresentes),
        tiposFaltantes,
        avisos,
        mensagem: "Importação concluída com sucesso.",
      }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (e) {
    return new Response(JSON.stringify({ error: String(e?.message ?? e) }), {
      status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
