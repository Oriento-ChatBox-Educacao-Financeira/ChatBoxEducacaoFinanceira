import { Injectable } from '@angular/core';

export interface RegisterIdentity {
  nome: string;
  nomeFantasia: string;
  cnpj: string;
}

@Injectable({ providedIn: 'root' })
export class RegisterStateService {
  private identity: RegisterIdentity | null = null;

  /**
   * Armazena os dados de identificação preenchidos na Tela 1.
   */
  setIdentity(data: RegisterIdentity): void {
    this.identity = data;
  }

  /**
   * Retorna os dados preenchidos na Tela 1, ou null se nada foi salvo.
   */
  getIdentity(): RegisterIdentity | null {
    return this.identity;
  }

  /**
   * Limpa o estado intermediário do cadastro.
   */
  clear(): void {
    this.identity = null;
  }
}
