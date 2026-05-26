import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { environment } from '../../environments/environment';
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
 * Cobre /api/dashboards/* do backend Spring. Tolerante a falhas — qualquer
 * erro \u00e9 logado e devolve hasData=false para a UI mostrar estado vazio
 * em vez de quebrar.
 */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly base = `${environment.apiUrl}/dashboards`;

  constructor(
    private http: HttpClient,
    private logger: LoggerService,
  ) {}

  dre(): Observable<DashboardDre> {
    return this.get<DashboardDre>('dre');
  }

  balanco(): Observable<DashboardBalanco> {
    return this.get<DashboardBalanco>('balanco');
  }

  fluxoCaixa(): Observable<DashboardFluxo> {
    return this.get<DashboardFluxo>('fluxo-caixa');
  }

  visaoGeral(): Observable<DashboardVisaoGeral> {
    return this.get<DashboardVisaoGeral>('visao-geral');
  }

  private get<T extends { hasData: boolean }>(path: string): Observable<T> {
    return this.http.get<T>(`${this.base}/${path}`).pipe(
      catchError((err) => {
        this.logger.warn?.(`Dashboard ${path} falhou`, err);
        return of({ hasData: false } as T);
      }),
    );
  }
}
