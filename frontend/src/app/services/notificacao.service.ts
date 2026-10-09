import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { LoggerService } from './logger.service';

export interface Notificacao {
  id_notificacao: number;
  tipo: 'PLANILHA_INCOMPLETA' | 'PLANILHA_IMPORTADA' | 'GERAL' | 'SISTEMA' | string;
  titulo: string;
  mensagem: string;
  link: string | null;
  icone: string | null;
  lida: boolean;
  data_criacao: string;
  data_leitura: string | null;
}

interface BackendNotificacao {
  idNotificacao: number;
  tipo: string;
  titulo: string;
  mensagem: string;
  link: string | null;
  icone: string | null;
  lida: boolean;
  dataCriacao: string;
  dataLeitura: string | null;
}

@Injectable({ providedIn: 'root' })
export class NotificacaoService {
  private readonly base = `${environment.apiUrl}/notificacoes`;

  readonly notificacoes = signal<Notificacao[]>([]);
  readonly naoLidas = computed(() => this.notificacoes().filter((n) => !n.lida).length);

  constructor(
    private http: HttpClient,
    private logger: LoggerService,
  ) {}

  async carregar(): Promise<void> {
    try {
      const data = await firstValueFrom(
        this.http.get<BackendNotificacao[]>(`${this.base}?limit=50`),
      );
      this.notificacoes.set((data ?? []).map(this.mapear));
    } catch (err) {
      this.logger.warn?.('Falha ao carregar notifica\u00e7\u00f5es', err);
    }
  }

  async marcarComoLida(id: number): Promise<void> {
    try {
      await firstValueFrom(this.http.patch<void>(`${this.base}/${id}/lida`, {}));
      this.notificacoes.update((arr) =>
        arr.map((n) => (n.id_notificacao === id ? { ...n, lida: true } : n)),
      );
    } catch (err) {
      this.logger.warn?.('Falha ao marcar como lida', err);
    }
  }

  async marcarTodasLidas(): Promise<void> {
    try {
      await firstValueFrom(this.http.patch<void>(`${this.base}/marcar-todas-lidas`, {}));
      this.notificacoes.update((arr) => arr.map((n) => ({ ...n, lida: true })));
    } catch (err) {
      this.logger.warn?.('Falha ao marcar todas como lidas', err);
    }
  }

  private mapear = (n: BackendNotificacao): Notificacao => ({
    id_notificacao: n.idNotificacao,
    tipo: n.tipo,
    titulo: n.titulo,
    mensagem: n.mensagem,
    link: n.link,
    icone: n.icone,
    lida: n.lida,
    data_criacao: n.dataCriacao,
    data_leitura: n.dataLeitura,
  });
}
