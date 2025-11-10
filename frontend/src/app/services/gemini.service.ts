import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { lastValueFrom } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class GeminiService {
  private backendUrl = 'https://oriento.ai/ask'; // URL do backend

  constructor(private http: HttpClient) {}

  async sendMessage(
    prompt: string,
    conversationId?: string
  ): Promise<{ reply: string; conversationId?: string }> {
    try {
      // Inclui o conversationId na requisição se disponível
      const url = conversationId
        ? `${this.backendUrl}?conversationId=${conversationId}`
        : this.backendUrl;

      const body = { prompt };

      const response = await lastValueFrom(
        this.http.post<{ reply: string; conversationId?: string }>(url, body)
      );

      return {
        reply: response.reply,
        conversationId: response.conversationId,
      };
    } catch (error) {
      console.error('Erro ao enviar mensagem para o backend:', error);
      return { reply: 'Erro ao se conectar com o servidor.', conversationId };
    }
  }
}
