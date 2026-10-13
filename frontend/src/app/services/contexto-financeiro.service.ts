import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { lastValueFrom } from 'rxjs';

import { environment } from '../../environments/environment';
import { LoggerService } from './logger.service';
import {
  ContextoFinanceiroFlag,
  ContextoFinanceiroRefreshResponse,
} from '../models/chat.model';

const STORAGE_KEY = 'oriento.contextoResumo.flag';
const TTL_MS = 24 * 60 * 60 * 1000;

/**
 * Aciona e gerencia o cache do "resumo financeiro" usado como contexto pelo
 * LLM. O backend gera o resumo automaticamente; aqui s\u00f3 controlamos
 * **quando** disparar o refresh, via flag em localStorage com TTL de 24h.
 */
@Injectable({ providedIn: 'root' })
export class ContextoFinanceiroService {
  private readonly endpoint = `${environment.apiUrl}/oriento/contexto/refresh`;

  constructor(
    private http: HttpClient,
    private logger: LoggerService,
  ) {}

  /**
   * Indica que precisamos chamar o backend para regenerar o resumo:
   * a flag est\u00e1 ausente, malformada ou expirada.
   */
  precisaRefresh(): boolean {
    const flag = this.lerFlag();
    if (!flag) return true;
    return Date.now() >= flag.validoAteMs;
  }

  /**
   * Chama POST /api/oriento/contexto/refresh e armazena uma flag local com TTL.
   * Tolera erros silenciosamente (apenas loga) para n\u00e3o quebrar o fluxo do chat.
   */
  async refresh(): Promise<void> {
    try {
      const resp = await lastValueFrom(
        this.http.post<ContextoFinanceiroRefreshResponse>(this.endpoint, {}),
      );
      const flag: ContextoFinanceiroFlag = {
        geradoEm: resp?.geradoEm ?? new Date().toISOString(),
        validoAteMs: Date.now() + TTL_MS,
      };
      this.salvarFlag(flag);
      this.logger.log('Contexto financeiro atualizado', flag);
    } catch (err) {
      this.logger.warn?.('Falha ao atualizar contexto financeiro', err);
    }
  }

  limpar(): void {
    if (typeof localStorage === 'undefined') return;
    localStorage.removeItem(STORAGE_KEY);
  }

  private lerFlag(): ContextoFinanceiroFlag | null {
    if (typeof localStorage === 'undefined') return null;
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    try {
      const parsed = JSON.parse(raw) as ContextoFinanceiroFlag;
      if (typeof parsed.validoAteMs !== 'number') return null;
      return parsed;
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }

  private salvarFlag(flag: ContextoFinanceiroFlag): void {
    if (typeof localStorage === 'undefined') return;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(flag));
  }
}
