import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { GeminiService } from '../../services/gemini.service';
import { MainNavbar } from '../main-navbar/main-navbar';

@Component({
  selector: 'app-chat-widget',
  imports: [FormsModule, CommonModule, MainNavbar],
  templateUrl: './chat-widget.html',
  styleUrl: './chat-widget.css',
})
export class ChatWidget {
  isOpen = false;
  userInput = '';
  messages: { sender: 'user' | 'bot'; text: string }[] = [];
  conversationId?: string; // Armazena o ID da conversa atual

  constructor(private geminiService: GeminiService) {}

  toggleChat() {
    this.isOpen = !this.isOpen;
  }

  async sendMessage() {
    const text = this.userInput.trim();
    if (!text) return;

    // Adiciona mensagem do usuário
    this.messages.push({ sender: 'user', text });
    this.userInput = '';

    this.messages.push({ sender: 'bot', text: 'Digitando...' });

    try {
      // Envia mensagem ao backend com o conversationId atual
      const result = await this.geminiService.sendMessage(text, this.conversationId);

      // Atualiza conversationId se o backend devolver um novo
      if (result.conversationId) {
        this.conversationId = result.conversationId;
        console.log('Novo conversationId recebido:', this.conversationId);
      }

      // Atualiza a última mensagem do bot com a resposta real
      this.messages[this.messages.length - 1] = {
        sender: 'bot',
        text: result.reply || 'Sem resposta.',
      };
    } catch (error) {
      console.error('Erro ao enviar mensagem:', error);
      this.messages[this.messages.length - 1] = {
        sender: 'bot',
        text: 'Erro ao se conectar com o servidor.',
      };
    }
  }

  trackByIndex(index: number) {
    return index;
  }

  sendSuggestion(text: string) {
    this.userInput = text;
    this.sendMessage();
  }
}
