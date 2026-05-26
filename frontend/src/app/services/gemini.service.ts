import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { lastValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { GeminiResponse } from '../models/chat.model';
import { LoggerService } from './logger.service';

@Injectable({
  providedIn: 'root',
})
export class GeminiService {
  private backendUrl = environment.geminiUrl;

  constructor(
    private http: HttpClient,
    private logger: LoggerService
  ) {}

  /**
   * Envia uma mensagem para o chatbot e retorna a resposta
   * @param prompt - A mensagem do usuário
   * @param conversationId - ID da conversa para manter contexto
   * @returns Promise com a resposta do bot e o ID da conversa
   */
  async sendMessage(prompt: string, conversationId?: string): Promise<GeminiResponse> {
    try {
      const options: { headers: Record<string, string>; params?: HttpParams } = {
        headers: { 'Content-Type': 'text/plain' },
      };

      if (conversationId) {
        options.params = new HttpParams().set('conversationId', conversationId);
      }

      this.logger.log('Enviando mensagem para Gemini', { conversationId });

      const response = await lastValueFrom(
        this.http.post<GeminiResponse>(this.backendUrl, prompt, options)
      );

      this.logger.log('Resposta recebida do Gemini');

      return {
        response: response.response || 'Sem resposta.',
        conversationId: response.conversationId,
      };
    } catch (error) {
      this.logger.error('Erro ao enviar mensagem para o backend', error);
      return {
        response: 'Erro ao se conectar com o servidor. Tente novamente.',
        conversationId,
      };
    }
  }
}
