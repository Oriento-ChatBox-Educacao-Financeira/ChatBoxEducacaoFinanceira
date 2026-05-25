import { Injectable, NgZone } from '@angular/core';
import { Observable } from 'rxjs';
import { SupabaseClientService } from './supabase.client';
import { LoggerService } from './logger.service';

export interface UploadProgress {
  status: 'uploading' | 'parsing' | 'done';
  percent: number;
  response?: UploadPlanilhaResponse;
}

export interface UploadPlanilhaResponse {
  arquivo: string;
  idEmpresa: number;
  linhasImportadas: number;
  mensagem: string;
  tiposPresentes?: string[];
  tiposFaltantes?: string[];
  avisos?: string[];
}

export class PlanilhaDuplicadaError extends Error {
  constructor(public mensagem: string, public arquivoOriginal?: string, public dataUpload?: string) {
    super(mensagem);
    this.name = 'PlanilhaDuplicadaError';
  }
}

export interface PlanilhaImportada {
  arquivo: string;
  hash: string | null;
  linhas: number;
  dataUpload: string;
}

async function sha256Hex(file: File): Promise<string> {
  const buf = await file.arrayBuffer();
  const digest = await crypto.subtle.digest('SHA-256', buf);
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

/**
 * Fluxo:
 *  1. Envia o arquivo bruto para o bucket Storage 'planilhas' em <userId>/<timestamp>-<nome>
 *  2. Invoca a Edge Function 'parse-planilha' passando o path
 *  3. A Edge Function baixa o arquivo, parseia (CSV/XLSX) e insere em public.linha_demonstrativo
 *  4. RLS garante que só o dono da empresa enxerga as proprias linhas
 */
@Injectable({ providedIn: 'root' })
export class PlanilhaService {
  constructor(
    private supabase: SupabaseClientService,
    private logger: LoggerService,
    private zone: NgZone,
  ) {}

  /**
   * Lista planilhas importadas (agrupa linha_demonstrativo por arquivo_origem).
   */
  async listar(): Promise<PlanilhaImportada[]> {
    const { data, error } = await this.supabase.client
      .from('linha_demonstrativo')
      .select('arquivo_origem, hash_arquivo, data_upload')
      .order('data_upload', { ascending: false });
    if (error) {
      this.logger.warn('Falha ao listar planilhas', error);
      return [];
    }
    const map = new Map<string, PlanilhaImportada>();
    for (const row of data ?? []) {
      const key = row.arquivo_origem ?? '(sem nome)';
      const ex = map.get(key);
      if (ex) {
        ex.linhas++;
      } else {
        map.set(key, {
          arquivo: key,
          hash: row.hash_arquivo,
          linhas: 1,
          dataUpload: row.data_upload,
        });
      }
    }
    return Array.from(map.values()).sort(
      (a, b) => +new Date(b.dataUpload) - +new Date(a.dataUpload),
    );
  }

  /**
   * Deleta todas as linhas de uma planilha (por arquivo_origem) e o arquivo
   * bruto no Storage. RLS garante que só o dono consegue apagar.
   */
  async deletar(arquivo: string): Promise<void> {
    const { error: delLinhasErr } = await this.supabase.client
      .from('linha_demonstrativo')
      .delete()
      .eq('arquivo_origem', arquivo);
    if (delLinhasErr) {
      throw new Error(`Falha ao apagar linhas: ${delLinhasErr.message}`);
    }

    // Apaga arquivo bruto do Storage (best-effort — caminho contém userId/timestamp-nome)
    const { data: session } = await this.supabase.client.auth.getSession();
    const userId = session.session?.user?.id;
    if (userId) {
      const { data: files } = await this.supabase.client.storage
        .from('planilhas')
        .list(userId, { limit: 1000 });
      const match = (files ?? []).filter((f) => f.name.endsWith(arquivo));
      if (match.length > 0) {
        const paths = match.map((f) => `${userId}/${f.name}`);
        await this.supabase.client.storage.from('planilhas').remove(paths);
      }
    }
  }

  upload(arquivo: File, idEmpresa?: number): Observable<UploadProgress> {
    return new Observable<UploadProgress>((sub) => {
      const emit = (ev: UploadProgress) => this.zone.run(() => sub.next(ev));
      const fail = (e: unknown) => this.zone.run(() => sub.error(e));
      const done = () => this.zone.run(() => sub.complete());

      (async () => {
        try {
          // 1) Identifica usuario autenticado
          const { data: session } = await this.supabase.client.auth.getSession();
          const userId = session.session?.user?.id;
          if (!userId) {
            throw new Error('Voce precisa estar autenticado para enviar planilhas.');
          }

          emit({ status: 'uploading', percent: 5 });

          // 2) Calcula hash SHA-256 do conteúdo para detecção de duplicata
          const hashArquivo = await sha256Hex(arquivo);
          emit({ status: 'uploading', percent: 20 });

          // 3) Upload bruto para o Storage
          const safeName = arquivo.name.replace(/[^a-zA-Z0-9._-]/g, '_');
          const path = `${userId}/${Date.now()}-${safeName}`;

          const { error: upErr } = await this.supabase.client.storage
            .from('planilhas')
            .upload(path, arquivo, {
              cacheControl: '3600',
              upsert: false,
              contentType: arquivo.type || undefined,
            });
          if (upErr) {
            throw new Error(`Falha no upload: ${upErr.message}`);
          }

          emit({ status: 'parsing', percent: 60 });

          // 4) Chama a Edge Function de parsing (passa o hash para detecção de duplicata server-side)
          const { data, error } = await this.supabase.client.functions.invoke<UploadPlanilhaResponse>(
            'parse-planilha',
            { body: { path, idEmpresa, hashArquivo } },
          );

          if (error) {
            // Edge function retornou 4xx/5xx — tenta extrair payload JSON
            const ctx = (error as { context?: Response }).context;
            if (ctx && typeof ctx.json === 'function') {
              try {
                const payload = await ctx.json();
                if (payload?.error === 'duplicada') {
                  throw new PlanilhaDuplicadaError(
                    payload.mensagem,
                    payload.arquivoOriginal,
                    payload.dataUpload,
                  );
                }
                throw new Error(payload?.error ?? payload?.mensagem ?? error.message);
              } catch (jsonErr) {
                if (jsonErr instanceof PlanilhaDuplicadaError) throw jsonErr;
              }
            }
            throw new Error(error.message ?? 'Falha ao processar planilha.');
          }
          if (!data) {
            throw new Error('Resposta vazia do servidor.');
          }

          this.logger.log('Upload concluido', data);
          emit({ status: 'done', percent: 100, response: data });
          done();
        } catch (e) {
          fail(e);
        }
      })();
    });
  }
}
