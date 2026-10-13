import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { lastValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { AiResponse } from '../models/chat.model';
import { AuthService } from './auth.service';
import { LoggerService } from './logger.service';

/**
 * Callbacks usados durante o streaming SSE do Oriento.
 * - onStart é disparado uma vez antes do primeiro delta (com o conversationId).
 * - onDelta é disparado para cada chunk de texto recebido.
 * - onDone é disparado quando o backend sinaliza fim da resposta.
 * - onError é disparado em qualquer falha terminal.
 */
export interface StreamCallbacks {
  onStart?: (conversationId: string | undefined) => void;
  onDelta?: (chunk: string) => void;
  onDone?: (conversationId: string | undefined) => void;
  onError?: (message: string) => void;
}

@Injectable({
  providedIn: 'root',
})
export class AiService {
  private backendUrl = environment.aiUrl;
  private streamUrl = `${environment.aiUrl}/stream`;

  constructor(
    private http: HttpClient,
    private auth: AuthService,
    private logger: LoggerService,
  ) {}

  /**
   * Versão síncrona — espera a resposta completa antes de retornar.
   * Mantida para compatibilidade; nova UI usa sendMessageStream.
   */
  async sendMessage(prompt: string, conversationId?: string): Promise<AiResponse> {
    try {
      const options: { headers: Record<string, string>; params?: HttpParams } = {
        headers: { 'Content-Type': 'text/plain' },
      };

      if (conversationId) {
        options.params = new HttpParams().set('conversationId', conversationId);
      }

      this.logger.log('Enviando mensagem para a AI', { conversationId });

      const response = await lastValueFrom(
        this.http.post<AiResponse>(this.backendUrl, prompt, options),
      );

      this.logger.log('Resposta recebida da AI');

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

  /**
   * Envia o prompt para o endpoint streaming SSE e dispara callbacks à medida
   * que os tokens chegam. O EventSource não permite POST com headers, então
   * usamos `fetch` + `ReadableStream` e parseamos o SSE manualmente.
   *
   * Retorna o AbortController para que o componente possa cancelar o stream
   * (por exemplo, quando o usuário troca de conversa).
   */
  sendMessageStream(
    prompt: string,
    conversationId: string | undefined,
    callbacks: StreamCallbacks,
  ): AbortController {
    const controller = new AbortController();

    const url = conversationId
      ? `${this.streamUrl}?conversationId=${encodeURIComponent(conversationId)}`
      : this.streamUrl;

    const token = this.auth.getAccessToken();
    const headers: Record<string, string> = {
      'Content-Type': 'text/plain',
      Accept: 'text/event-stream',
    };
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    this.logger.log('Iniciando stream do Oriento', { conversationId });

    fetch(url, {
      method: 'POST',
      headers,
      body: prompt,
      signal: controller.signal,
      credentials: 'include',
    })
      .then(async (response) => {
        if (!response.ok || !response.body) {
          const text = await safeReadText(response);
          throw new Error(text || `HTTP ${response.status}`);
        }
        await this.parseStream(response.body, callbacks);
      })
      .catch((err: unknown) => {
        if ((err as DOMException)?.name === 'AbortError') {
          this.logger.log('Stream cancelado pelo cliente');
          return;
        }
        const message = err instanceof Error ? err.message : 'Falha ao conectar';
        this.logger.error('Erro no stream do Oriento', err);
        callbacks.onError?.(message);
      });

    return controller;
  }

  /**
   * Lê o ReadableStream do fetch, quebra o conteúdo em eventos SSE
   * (separados por linha em branco), parseia o `event:` + `data:` e dispara
   * o callback adequado.
   */
  private async parseStream(
    body: ReadableStream<Uint8Array>,
    callbacks: StreamCallbacks,
  ): Promise<void> {
    const reader = body.getReader();
    const decoder = new TextDecoder('utf-8');
    let buffer = '';

    try {
      while (true) {
        const { value, done } = await reader.read();
        if (done) {
          break;
        }
        buffer += decoder.decode(value, { stream: true });

        let separatorIndex: number;
        // SSE separa eventos por linha em branco (\n\n).
        while ((separatorIndex = buffer.indexOf('\n\n')) !== -1) {
          const rawEvent = buffer.slice(0, separatorIndex);
          buffer = buffer.slice(separatorIndex + 2);
          this.dispatchEvent(rawEvent, callbacks);
        }
      }
      // Drena qualquer evento residual sem newline final
      if (buffer.trim().length > 0) {
        this.dispatchEvent(buffer, callbacks);
      }
    } finally {
      reader.releaseLock();
    }
  }

  private dispatchEvent(rawEvent: string, callbacks: StreamCallbacks): void {
    const lines = rawEvent.split('\n');
    let eventName = 'message';
    const dataParts: string[] = [];

    for (const line of lines) {
      if (line.startsWith(':') || line.length === 0) {
        continue;
      }
      const colonIndex = line.indexOf(':');
      const field = colonIndex === -1 ? line : line.slice(0, colonIndex);
      const valueRaw = colonIndex === -1 ? '' : line.slice(colonIndex + 1);
      const value = valueRaw.startsWith(' ') ? valueRaw.slice(1) : valueRaw;

      if (field === 'event') {
        eventName = value;
      } else if (field === 'data') {
        dataParts.push(value);
      }
    }

    if (dataParts.length === 0) {
      return;
    }

    const dataString = dataParts.join('\n');
    let payload: Record<string, unknown> = {};
    try {
      payload = JSON.parse(dataString);
    } catch {
      payload = { raw: dataString };
    }

    switch (eventName) {
      case 'start':
        callbacks.onStart?.((payload['conversationId'] as string | undefined) ?? undefined);
        break;
      case 'delta': {
        const content = (payload['content'] as string | undefined) ?? '';
        if (content) {
          callbacks.onDelta?.(content);
        }
        break;
      }
      case 'done':
        callbacks.onDone?.((payload['conversationId'] as string | undefined) ?? undefined);
        break;
      case 'error': {
        const message = (payload['message'] as string | undefined) ?? 'Erro desconhecido';
        callbacks.onError?.(message);
        break;
      }
      default:
        // Ignora eventos não reconhecidos
        break;
    }
  }
}

async function safeReadText(response: Response): Promise<string> {
  try {
    return await response.text();
  } catch {
    return '';
  }
}
