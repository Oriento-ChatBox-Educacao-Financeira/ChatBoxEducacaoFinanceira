import { Injectable, signal, computed } from '@angular/core';
import { SupabaseClientService } from './supabase.client';
import { LoggerService } from './logger.service';

export interface Notificacao {
  id_notificacao: number;
  tipo: 'PLANILHA_INCOMPLETA' | 'PLANILHA_IMPORTADA' | 'GERAL' | 'SISTEMA';
  titulo: string;
  mensagem: string;
  link: string | null;
  icone: string | null;
  lida: boolean;
  data_criacao: string;
  data_leitura: string | null;
}

@Injectable({ providedIn: 'root' })
export class NotificacaoService {
  readonly notificacoes = signal<Notificacao[]>([]);
  readonly naoLidas = computed(() => this.notificacoes().filter((n) => !n.lida).length);

  constructor(
    private supabase: SupabaseClientService,
    private logger: LoggerService,
  ) {}

  async carregar(): Promise<void> {
    const { data, error } = await this.supabase.client
      .from('notificacao')
      .select('*')
      .order('data_criacao', { ascending: false })
      .limit(50);
    if (error) {
      this.logger.warn('Falha ao carregar notificações', error);
      return;
    }
    this.notificacoes.set((data ?? []) as Notificacao[]);
  }

  async marcarComoLida(id: number): Promise<void> {
    const { error } = await this.supabase.client
      .from('notificacao')
      .update({ lida: true, data_leitura: new Date().toISOString() })
      .eq('id_notificacao', id);
    if (error) {
      this.logger.warn('Falha ao marcar notificação como lida', error);
      return;
    }
    this.notificacoes.update((arr) =>
      arr.map((n) => (n.id_notificacao === id ? { ...n, lida: true } : n)),
    );
  }

  async marcarTodasLidas(): Promise<void> {
    const ids = this.notificacoes()
      .filter((n) => !n.lida)
      .map((n) => n.id_notificacao);
    if (!ids.length) return;
    const { error } = await this.supabase.client
      .from('notificacao')
      .update({ lida: true, data_leitura: new Date().toISOString() })
      .in('id_notificacao', ids);
    if (error) {
      this.logger.warn('Falha ao marcar todas como lidas', error);
      return;
    }
    this.notificacoes.update((arr) => arr.map((n) => ({ ...n, lida: true })));
  }
}
