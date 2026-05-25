import { Injectable } from '@angular/core';
import { from, Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { SupabaseClientService } from './supabase.client';
import { LoggerService } from './logger.service';

export interface DreItem {
  label: string;
  valor: number;
  bold?: boolean;
  sub?: boolean;
  blue?: boolean;
  total?: boolean;
}

export interface DashboardDre {
  hasData: boolean;
  kpis?: {
    receitaBruta: number;
    despOperacionais: number;
    lucroLiquido: number;
    margemLucro: number;
  };
  itens?: DreItem[];
  evolucao?: { mes: string; receita: number; despesa: number }[];
}

export interface BalancoItem {
  nome: string;
  desc: string;
  valor: number;
}

export interface DashboardBalanco {
  hasData: boolean;
  kpis?: {
    totalAtivo: number;
    totalPassivo: number;
    patrimonioLiquido: number;
    liquidez: number | null;
  };
  ativos?: BalancoItem[];
  recursos?: BalancoItem[];
  evolucao?: { rotulo: string; patrimonio: number }[];
}

export interface MovimentacaoItem {
  descricao: string;
  valor: number;
  periodo: string;
  entrada: boolean;
}

export interface DashboardFluxo {
  hasData: boolean;
  kpis?: {
    saldoAtual: number;
    entradas: number;
    saidas: number;
    projecao30dias: number;
  };
  movimentacoes?: MovimentacaoItem[];
  evolucao?: { mes: string; entrada: number; saida: number }[];
}

export interface DashboardVisaoGeral {
  hasData: boolean;
  kpis?: {
    saldoAtual: number;
    lucroLiquido: number;
    margem: number;
    patrimonioLiquido: number;
    liquidez: number;
  };
  evolucao?: { mes: string; receita: number; despesa: number }[];
}

/**
 * Consume as RPCs do Postgres (Supabase) que agregam os dados da tabela
 * linha_demonstrativo por empresa do usuario autenticado.
 */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  constructor(
    private supabase: SupabaseClientService,
    private logger: LoggerService,
  ) {}

  dre(idEmpresa?: number): Observable<DashboardDre> {
    return this.callRpc<DashboardDre>('dashboard_dre', idEmpresa);
  }

  balanco(idEmpresa?: number): Observable<DashboardBalanco> {
    return this.callRpc<DashboardBalanco>('dashboard_balanco', idEmpresa);
  }

  fluxoCaixa(idEmpresa?: number): Observable<DashboardFluxo> {
    return this.callRpc<DashboardFluxo>('dashboard_fluxo_caixa', idEmpresa);
  }

  visaoGeral(idEmpresa?: number): Observable<DashboardVisaoGeral> {
    return this.callRpc<DashboardVisaoGeral>('dashboard_visao_geral', idEmpresa);
  }

  private callRpc<T>(fn: string, idEmpresa?: number): Observable<T> {
    const params = idEmpresa != null ? { p_id_empresa: idEmpresa } : {};
    return from(this.supabase.client.rpc(fn, params)).pipe(
      map(({ data, error }) => {
        if (error) {
          this.logger.error(`RPC ${fn} falhou`, error);
          throw error;
        }
        return (data ?? { hasData: false }) as T;
      }),
      catchError((err) => {
        this.logger.warn?.(`Fallback ${fn} sem dados: ${err?.message ?? err}`);
        return of({ hasData: false } as T);
      }),
    );
  }
}
