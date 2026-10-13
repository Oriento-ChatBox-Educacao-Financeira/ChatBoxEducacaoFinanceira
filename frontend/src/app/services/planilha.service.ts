import { Injectable } from '@angular/core';
import { HttpClient, HttpEventType } from '@angular/common/http';
import { Observable, firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
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
  constructor(
    public mensagem: string,
    public arquivoOriginal?: string,
    public dataUpload?: string,
  ) {
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

/**
 * Cobre /api/planilhas/* do backend. O backend cuida do parsing
 * XLSX/CSV, dedupe por chave natural e gera notifica\u00e7\u00e3o autom\u00e1tica
 * quando faltam tipos de demonstrativo (DRE/BP/FluxoCaixa).
 */
@Injectable({ providedIn: 'root' })
export class PlanilhaService {
  private readonly base = `${environment.apiUrl}/planilhas`;

  constructor(
    private http: HttpClient,
    private logger: LoggerService,
  ) {}

  async listar(): Promise<PlanilhaImportada[]> {
    try {
      const data = await firstValueFrom(this.http.get<PlanilhaImportada[]>(this.base));
      return data ?? [];
    } catch (err) {
      this.logger.warn?.('Falha ao listar planilhas', err);
      return [];
    }
  }

  async deletar(arquivo: string): Promise<void> {
    await firstValueFrom(
      this.http.delete<void>(`${this.base}/${encodeURIComponent(arquivo)}`),
    );
  }

  upload(arquivo: File): Observable<UploadProgress> {
    return new Observable<UploadProgress>((sub) => {
      const formData = new FormData();
      formData.append('arquivo', arquivo);

      const subscription = this.http
        .post<UploadPlanilhaResponse>(`${this.base}/upload`, formData, {
          reportProgress: true,
          observe: 'events',
        })
        .subscribe({
          next: (event) => {
            if (event.type === HttpEventType.UploadProgress) {
              if (event.total) {
                const percent = Math.min(95, Math.round((event.loaded / event.total) * 90));
                sub.next({ status: 'uploading', percent });
              }
            } else if (event.type === HttpEventType.Response) {
              const response = event.body!;
              sub.next({ status: 'parsing', percent: 95 });
              sub.next({ status: 'done', percent: 100, response });
              sub.complete();
            }
          },
          error: (err) => {
            const payload = err?.error;
            if (err?.status === 409 && payload?.error === 'duplicada') {
              sub.error(
                new PlanilhaDuplicadaError(
                  payload.message ?? 'Planilha j\u00e1 importada anteriormente.',
                  payload.arquivoOriginal,
                ),
              );
              return;
            }
            sub.error(new Error(payload?.message ?? err?.message ?? 'Falha no upload.'));
          },
        });

      return () => subscription.unsubscribe();
    });
  }
}
