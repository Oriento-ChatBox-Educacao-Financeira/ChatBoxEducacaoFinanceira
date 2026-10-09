import {
  AfterViewInit,
  Component,
  ElementRef,
  NgZone,
  OnDestroy,
  ViewChild,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

import { AiService } from '../../services/ai.service';
import { LoggerService } from '../../services/logger.service';

import { ChatMessage } from '../../models/chat.model';
import { MarkdownPipe } from '../../pipes/markdown.pipe';

const STICK_TO_BOTTOM_TOLERANCE_PX = 80;

@Component({
  selector: 'app-chat-widget',
  standalone: true,
  imports: [FormsModule, CommonModule, MarkdownPipe],
  templateUrl: './chat-widget.html',
  styleUrl: './chat-widget.css',
})
export class ChatWidget implements AfterViewInit, OnDestroy {
  @ViewChild('chatBody') chatBody?: ElementRef<HTMLElement>;

  isOpen = false;

  userInput = '';

  messages = signal<ChatMessage[]>([]);

  isTyping = signal(false);

  conversationId?: string;

  isSending = false;

  private readonly MAX_MESSAGES_PER_MINUTE = 10;
  private readonly RATE_LIMIT_WINDOW = 60000;
  private messageTimes: number[] = [];

  private streamController?: AbortController;

  // Buffer de deltas — flush em requestAnimationFrame para Markdown live fluido.
  private deltaBuffer = '';
  private flushScheduled = false;
  // Acompanha o fim da bolha automaticamente, a menos que o user role para cima.
  private stickToBottom = true;
  private scrollHandler?: () => void;

  constructor(
    private aiService: AiService,
    private logger: LoggerService,
    private router: Router,
    private zone: NgZone,
  ) {}

  toggleChat(): void {
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      this.stickToBottom = true;
      queueMicrotask(() => this.scrollToBottom(true));
    }
  }

  openFullChat(): void {
    this.router.navigate(['/chat']);
  }

  ngAfterViewInit(): void {
    this.attachScrollListener();
  }

  ngOnDestroy(): void {
    this.streamController?.abort();
    this.detachScrollListener();
  }

  private attachScrollListener(): void {
    const el = this.chatBody?.nativeElement;
    if (!el) return;
    this.scrollHandler = () => {
      const distance = el.scrollHeight - el.scrollTop - el.clientHeight;
      this.stickToBottom = distance < STICK_TO_BOTTOM_TOLERANCE_PX;
    };
    // Escuta passivo, fora do Angular para não disparar change detection à toa.
    this.zone.runOutsideAngular(() =>
      el.addEventListener('scroll', this.scrollHandler!, { passive: true }),
    );
  }

  private detachScrollListener(): void {
    const el = this.chatBody?.nativeElement;
    if (el && this.scrollHandler) {
      el.removeEventListener('scroll', this.scrollHandler);
    }
  }

  private scrollToBottom(force = false): void {
    const el = this.chatBody?.nativeElement;
    if (!el) return;
    if (!force && !this.stickToBottom) return;
    el.scrollTop = el.scrollHeight;
  }

  private isRateLimited(): boolean {
    const now = Date.now();
    this.messageTimes = this.messageTimes.filter((time) => now - time < this.RATE_LIMIT_WINDOW);
    return this.messageTimes.length >= this.MAX_MESSAGES_PER_MINUTE;
  }

  private recordMessageTime(): void {
    this.messageTimes.push(Date.now());
  }

  private appendMessage(msg: ChatMessage): void {
    this.messages.update((arr) => [...arr, msg]);
    this.stickToBottom = true;
    this.scheduleScroll();
  }

  /**
   * Atualiza a última mensagem (bolha do bot em streaming) recriando a
   * referência para que o pipe Markdown puro reprocesse o texto.
   */
  private updateLastBotMessage(updater: (msg: ChatMessage) => ChatMessage): void {
    this.messages.update((arr) => {
      if (arr.length === 0) return arr;
      const copy = arr.slice();
      const last = copy[copy.length - 1];
      if (last.sender !== 'bot') return arr;
      copy[copy.length - 1] = updater(last);
      return copy;
    });
  }

  /**
   * Acumula os chunks recebidos e faz um único update de signal por frame de
   * animação. Isso evita reparse de Markdown a cada token (5-30/s) e mantém
   * a UI fluida mesmo com o LLM cuspindo deltas pequenos.
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

  /**
   * Agenda o scroll para o próximo frame — depois que o Angular rodar a
   * change detection e o navegador terminar o reflow, garantindo
   * scrollHeight atualizado.
   */
  private scheduleScroll(): void {
    this.zone.runOutsideAngular(() => {
      requestAnimationFrame(() => this.scrollToBottom());
    });
  }

  sendMessage(): void {
    const text = this.userInput.trim();
    if (!text || this.isSending) return;

    if (this.isRateLimited()) {
      this.appendMessage({
        sender: 'bot',
        text: 'Você atingiu o limite de mensagens por minuto.',
        timestamp: new Date(),
      });
      return;
    }

    this.isSending = true;
    this.recordMessageTime();

    this.appendMessage({
      sender: 'user',
      text,
      timestamp: new Date(),
    });

    this.userInput = '';
    this.isTyping.set(true);
    this.stickToBottom = true;

    this.appendMessage({
      sender: 'bot',
      text: '',
      timestamp: new Date(),
      pending: true,
      streaming: true,
    });

    this.streamController?.abort();
    this.deltaBuffer = '';
    this.flushScheduled = false;
    this.streamController = this.aiService.sendMessageStream(text, this.conversationId, {
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
          timestamp: new Date(),
        }));
        this.isTyping.set(false);
        this.isSending = false;
        this.streamController = undefined;
        this.scheduleScroll();
      },
      onError: (message) => {
        this.logger.error('Erro no stream do widget', message);
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

  sendSuggestion(text: string): void {
    this.userInput = text;
    this.sendMessage();
  }

  trackByIndex(index: number): number {
    return index;
  }
}
