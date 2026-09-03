import {
  AfterViewInit,
  Component,
  ElementRef,
  NgZone,
  OnDestroy,
  OnInit,
  ViewChild,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { MainNavbar } from '../../_components/main-navbar/main-navbar';
import { AiService } from '../../services/ai.service';
import { LoggerService } from '../../services/logger.service';
import { ConversasService } from '../../services/conversas.service';
import { ContextoFinanceiroService } from '../../services/contexto-financeiro.service';
import { MarkdownPipe } from '../../pipes/markdown.pipe';
import { ConversaResumo, MensagemHistorico } from '../../models/chat.model';

interface PageChatMessage {
  role: 'user' | 'bot';
  text: string;
  time: string;
  pending?: boolean;
  streaming?: boolean;
}

const STICK_TO_BOTTOM_TOLERANCE_PX = 80;

@Component({
  selector: 'app-chat-page',
  standalone: true,
  imports: [CommonModule, FormsModule, MainNavbar, MarkdownPipe],
  templateUrl: './chat.html',
  styleUrl: './chat.css',
})
export class ChatPageComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('scrollEl') scrollEl?: ElementRef<HTMLElement>;
  @ViewChild('textarea') textarea?: ElementRef<HTMLTextAreaElement>;

  message = '';
  drawerOpen = signal(false);

  isTyping = signal(false);
  isSending = false;
  isCarregandoHistorico = signal(false);
  conversationId?: string;

  suggestions: string[] = [
    'Explicar minha DRE',
    'Projetar fluxo de caixa',
    'Analisar margem operacional',
    'Reduzir despesas',
  ];

  conversations = signal<ConversaResumo[]>([]);

  messages = signal<PageChatMessage[]>([]);

  private readonly MAX_MESSAGES_PER_MINUTE = 10;
  private readonly RATE_LIMIT_WINDOW = 60000;
  private messageTimes: number[] = [];
  private streamController?: AbortController;

  // Buffer de deltas — flush em rAF para Markdown live fluido.
  private deltaBuffer = '';
  private flushScheduled = false;
  // Acompanha o fim a menos que o usuário role para cima.
  private stickToBottom = true;
  private scrollHandler?: () => void;

  constructor(
    private aiService: AiService,
    private logger: LoggerService,
    private conversasService: ConversasService,
    private contextoService: ContextoFinanceiroService,
    private route: ActivatedRoute,
    private router: Router,
    private zone: NgZone,
  ) {}

  async ngOnInit(): Promise<void> {
    if (this.contextoService.precisaRefresh()) {
      void this.contextoService.refresh();
    }

    await this.recarregarConversas();

    this.route.queryParamMap.subscribe(async (params) => {
      const conversaIdParam = params.get('conversaId');
      const perguntaParam = params.get('pergunta');

      if (conversaIdParam) {
        const found = this.conversations().find((c) => c.id === conversaIdParam);
        if (found) {
          await this.abrirConversa(found);
        }
      }

      if (perguntaParam) {
        this.message = perguntaParam;
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: { pergunta: null, conversaId: null },
          queryParamsHandling: 'merge',
          replaceUrl: true,
        });
        setTimeout(() => this.enviar(), 0);
      }
    });
  }

  ngAfterViewInit(): void {
    this.attachScrollListener();
  }

  private attachScrollListener(): void {
    const el = this.scrollEl?.nativeElement;
    if (!el) return;
    this.scrollHandler = () => {
      const distance = el.scrollHeight - el.scrollTop - el.clientHeight;
      this.stickToBottom = distance < STICK_TO_BOTTOM_TOLERANCE_PX;
    };
    this.zone.runOutsideAngular(() =>
      el.addEventListener('scroll', this.scrollHandler!, { passive: true }),
    );
  }

  private detachScrollListener(): void {
    const el = this.scrollEl?.nativeElement;
    if (el && this.scrollHandler) {
      el.removeEventListener('scroll', this.scrollHandler);
    }
  }

  private scrollToBottom(force = false): void {
    const el = this.scrollEl?.nativeElement;
    if (!el) return;
    if (!force && !this.stickToBottom) return;
    el.scrollTop = el.scrollHeight;
  }

  /**
   * Agenda o scroll para o próximo frame — depois que o Angular rodar
   * change detection e o navegador atualizar o reflow, garantindo
   * scrollHeight atualizado.
   */
  private scheduleScroll(force = false): void {
    this.zone.runOutsideAngular(() => {
      requestAnimationFrame(() => this.scrollToBottom(force));
    });
  }

  sendSuggestion(text: string): void {
    this.message = text;
    this.textarea?.nativeElement.focus();
    this.autoResize();
    this.enviar();
  }

  private isRateLimited(): boolean {
    const now = Date.now();
    this.messageTimes = this.messageTimes.filter((t) => now - t < this.RATE_LIMIT_WINDOW);
    return this.messageTimes.length >= this.MAX_MESSAGES_PER_MINUTE;
  }

  private nowTime(): string {
    const now = new Date();
    return `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
  }

  private appendMessage(msg: PageChatMessage): void {
    this.messages.update((arr) => [...arr, msg]);
    this.stickToBottom = true;
    this.scheduleScroll();
  }

  /**
   * Acumula chunks recebidos e faz um único update de signal por frame
   * de animação. Evita reparse de Markdown a cada token.
   */
  private bufferDelta(chunk: string): void {
    this.deltaBuffer += chunk;
    if (this.flushScheduled) return;
    this.flushScheduled = true;
    this.zone.runOutsideAngular(() => {
      requestAnimationFrame(() => {
        this.zone.run(() => this.flushDeltaBuffer());
      });
    });
  }

  private flushDeltaBuffer(): void {
    this.flushScheduled = false;
    if (!this.deltaBuffer) return;
    const chunk = this.deltaBuffer;
    this.deltaBuffer = '';
    this.updateLastBotMessage((msg) => ({
      ...msg,
      text: msg.text + chunk,
      pending: false,
      streaming: true,
    }));
    this.isTyping.set(false);
    this.scheduleScroll();
  }

  enviar(): void {
    const txt = this.message.trim();
    if (!txt || this.isSending) return;

    if (this.isRateLimited()) {
      this.appendMessage({
        role: 'bot',
        text: 'Você atingiu o limite de mensagens por minuto. Aguarde antes de tentar novamente.',
        time: this.nowTime(),
      });
      return;
    }

    this.isSending = true;
    this.messageTimes.push(Date.now());

    this.appendMessage({
      role: 'user',
      text: txt,
      time: this.nowTime(),
    });

    this.message = '';
    setTimeout(() => this.autoResize());

    this.isTyping.set(true);
    this.stickToBottom = true;

    const novaConversaIniciada = !this.conversationId;

    this.appendMessage({
      role: 'bot',
      text: '',
      time: this.nowTime(),
      pending: true,
      streaming: true,
    });

    this.streamController?.abort();
    this.deltaBuffer = '';
    this.flushScheduled = false;
    this.streamController = this.aiService.sendMessageStream(txt, this.conversationId, {
      onStart: (id) => {
        if (id) this.conversationId = id;
      },
      onDelta: (chunk) => this.bufferDelta(chunk),
      onDone: (id) => {
        if (id) this.conversationId = id;
        this.flushDeltaBuffer();
        this.updateLastBotMessage((msg) => ({
          ...msg,
          pending: false,
          streaming: false,
          time: this.nowTime(),
        }));
        this.isTyping.set(false);
        this.isSending = false;
        this.streamController = undefined;
        this.scheduleScroll();
        if (novaConversaIniciada) {
          void this.recarregarConversas();
        }
      },
      onError: (message) => {
        this.logger.error('Erro no stream do Oriento', message);
        this.flushDeltaBuffer();
        this.updateLastBotMessage((msg) => ({
          ...msg,
          text: msg.text || 'Erro ao se conectar com o servidor. Tente novamente.',
          pending: false,
          streaming: false,
        }));
        this.isTyping.set(false);
        this.isSending = false;
        this.streamController = undefined;
      },
    });
  }

  private updateLastBotMessage(updater: (msg: PageChatMessage) => PageChatMessage): void {
    this.messages.update((arr) => {
      if (arr.length === 0) return arr;
      const copy = arr.slice();
      const last = copy[copy.length - 1];
      if (last.role !== 'bot') return arr;
      copy[copy.length - 1] = updater(last);
      return copy;
    });
  }

  ngOnDestroy(): void {
    this.streamController?.abort();
    this.detachScrollListener();
  }

  onKeydown(ev: KeyboardEvent): void {
    if (ev.key === 'Enter' && !ev.shiftKey) {
      ev.preventDefault();
      this.enviar();
    }
  }

  autoResize(): void {
    const el = this.textarea?.nativeElement;
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 200) + 'px';
  }

  novaConversa(): void {
    this.streamController?.abort();
    this.streamController = undefined;
    this.deltaBuffer = '';
    this.flushScheduled = false;
    this.messages.set([]);
    this.conversationId = undefined;
    this.message = '';
    this.isTyping.set(false);
    this.isSending = false;
    this.stickToBottom = true;
  }

  async abrirConversa(c: ConversaResumo): Promise<void> {
    if (this.isCarregandoHistorico()) return;

    this.streamController?.abort();
    this.streamController = undefined;
    this.isSending = false;
    this.isTyping.set(false);

    this.isCarregandoHistorico.set(true);
    try {
      const historico = await firstValueFrom(this.conversasService.historico(c.id));
      const mapped = this.mapearHistorico(historico);
      this.conversationId = c.id;
      this.messages.set(mapped);
      this.stickToBottom = true;
      this.scheduleScroll(true);
      this.fecharDrawer();
    } catch (err) {
      this.logger.error('Erro ao carregar histórico da conversa', err);
      this.appendMessage({
        role: 'bot',
        text: 'Não foi possível carregar essa conversa. Tente novamente.',
        time: this.nowTime(),
      });
    } finally {
      this.isCarregandoHistorico.set(false);
    }
  }

  isConversaAtiva(c: ConversaResumo): boolean {
    return this.conversationId === c.id;
  }

  formatarData(iso: string | undefined): string {
    if (!iso) return '';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '';
    const hoje = new Date();
    const ontem = new Date();
    ontem.setDate(hoje.getDate() - 1);
    const sameDay = (a: Date, b: Date) =>
      a.getFullYear() === b.getFullYear() &&
      a.getMonth() === b.getMonth() &&
      a.getDate() === b.getDate();
    if (sameDay(d, hoje)) return 'Hoje';
    if (sameDay(d, ontem)) return 'Ontem';
    return d.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' });
  }

  trackByIndex(index: number): number {
    return index;
  }

  trackByConversa(_: number, c: ConversaResumo): string {
    return c.id;
  }

  toggleDrawer(): void {
    this.drawerOpen.update((v) => !v);
  }

  fecharDrawer(): void {
    this.drawerOpen.set(false);
  }

  private async recarregarConversas(): Promise<void> {
    try {
      const lista = await firstValueFrom(this.conversasService.listar());
      this.conversations.set(lista ?? []);
    } catch (err) {
      this.logger.warn?.('Falha ao listar conversas', err);
      this.conversations.set([]);
    }
  }

  private mapearHistorico(historico: MensagemHistorico[]): PageChatMessage[] {
    return (historico ?? [])
      .slice()
      .sort((a, b) => (a.ordem ?? 0) - (b.ordem ?? 0))
      .map((m) => ({
        role: m.remetente === 'usuario' ? ('user' as const) : ('bot' as const),
        text: m.conteudo?.texto ?? '',
        time: m.dataHora ? this.horaDeIso(m.dataHora) : '',
      }))
      .filter((m) => m.text.length > 0);
  }

  private horaDeIso(iso: string): string {
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '';
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  }
}
