import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

import { GeminiService } from '../../services/gemini.service';
import { LoggerService } from '../../services/logger.service';

import { ChatMessage } from '../../models/chat.model';

@Component({
  selector: 'app-chat-widget',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './chat-widget.html',
  styleUrl: './chat-widget.css',
})
export class ChatWidget {
  isOpen = false;

  userInput = '';

  messages: ChatMessage[] = [];

  conversationId?: string;

  isSending = false;

  private readonly MAX_MESSAGES_PER_MINUTE = 10;

  private readonly RATE_LIMIT_WINDOW = 60000;

  private messageTimes: number[] = [];

  constructor(
    private geminiService: GeminiService,
    private logger: LoggerService,
    private router: Router,
  ) {}

  toggleChat(): void {
    this.isOpen = !this.isOpen;
  }

  openFullChat(): void {
    this.router.navigate(['/chat']);
  }

  private isRateLimited(): boolean {
    const now = Date.now();

    this.messageTimes = this.messageTimes.filter((time) => now - time < this.RATE_LIMIT_WINDOW);

    return this.messageTimes.length >= this.MAX_MESSAGES_PER_MINUTE;
  }

  private recordMessageTime(): void {
    this.messageTimes.push(Date.now());
  }

  async sendMessage(): Promise<void> {
    const text = this.userInput.trim();

    if (!text || this.isSending) {
      return;
    }

    if (this.isRateLimited()) {
      this.messages.push({
        sender: 'bot',
        text: 'Você atingiu o limite de mensagens por minuto.',
      });

      return;
    }

    this.isSending = true;

    this.recordMessageTime();

    this.messages.push({
      sender: 'user',
      text,
      timestamp: new Date(),
    });

    this.userInput = '';

    this.messages.push({
      sender: 'bot',
      text: 'Digitando...',
      timestamp: new Date(),
    });

    try {
      const result = await this.geminiService.sendMessage(text, this.conversationId);

      if (result.conversationId) {
        this.conversationId = result.conversationId;
      }

      this.messages[this.messages.length - 1] = {
        sender: 'bot',
        text: result.response,
        timestamp: new Date(),
      };
    } catch (error) {
      this.logger.error('Erro ao enviar mensagem', error);

      this.messages[this.messages.length - 1] = {
        sender: 'bot',
        text: 'Erro ao se conectar com o servidor.',
        timestamp: new Date(),
      };
    } finally {
      this.isSending = false;
    }
  }

  sendSuggestion(text: string): void {
    this.userInput = text;

    this.sendMessage();
  }

  trackByIndex(index: number): number {
    return index;
  }
}
