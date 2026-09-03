import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { ConversaResumo, MensagemHistorico } from '../models/chat.model';

/**
 * Conversa com os endpoints de hist\u00f3rico de chat:
 * - GET /api/conversas (lista do usu\u00e1rio autenticado)
 * - GET /api/mensagens/conversa/{id} (mensagens de uma conversa, com ownership check)
 */
@Injectable({ providedIn: 'root' })
export class ConversasService {
  private readonly conversasBase = `${environment.apiUrl}/conversas`;
  private readonly mensagensBase = `${environment.apiUrl}/mensagens`;

  constructor(private http: HttpClient) {}

  listar(): Observable<ConversaResumo[]> {
    return this.http.get<ConversaResumo[]>(this.conversasBase);
  }

  historico(conversaId: string): Observable<MensagemHistorico[]> {
    return this.http.get<MensagemHistorico[]>(`${this.mensagensBase}/conversa/${conversaId}`);
  }
}
