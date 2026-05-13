import { Injectable } from '@angular/core';

export interface RegisterIdentity {
  nome: string;
  nomeFantasia: string;
  cnpj: string;
}

export interface RegisterCredentials {
  email: string;
  confirmarEmail: string;
  aceiteTermos: boolean;
}

const IDENTITY_KEY = 'oriento.register.identity';
const CREDENTIALS_KEY = 'oriento.register.credentials';

@Injectable({ providedIn: 'root' })
export class RegisterStateService {
  /**
   * Armazena os dados de identificação preenchidos na Tela 1.
   * Usa sessionStorage para sobreviver a refresh durante o fluxo de cadastro
   * sem persistir além da aba.
   */
  setIdentity(data: RegisterIdentity): void {
    this.write(IDENTITY_KEY, data);
  }

  getIdentity(): RegisterIdentity | null {
    return this.read<RegisterIdentity>(IDENTITY_KEY);
  }

  /**
   * Armazena os dados não-sensíveis da Tela 2 (e-mail e aceite) para permitir
   * voltar à Tela 1 e retomar o cadastro sem redigitá-los.
   * Senha/confirmarSenha NÃO são persistidas para evitar exposição via
   * XSS/extensões inspecionando o sessionStorage.
   */
  setCredentials(data: RegisterCredentials): void {
    this.write(CREDENTIALS_KEY, data);
  }

  getCredentials(): RegisterCredentials | null {
    return this.read<RegisterCredentials>(CREDENTIALS_KEY);
  }

  clear(): void {
    this.remove(IDENTITY_KEY);
    this.remove(CREDENTIALS_KEY);
  }

  private write(key: string, value: unknown): void {
    if (typeof sessionStorage === 'undefined') return;
    try {
      sessionStorage.setItem(key, JSON.stringify(value));
    } catch {
      // sessionStorage indisponível (modo privado, quota): segue sem persistir.
    }
  }

  private read<T>(key: string): T | null {
    if (typeof sessionStorage === 'undefined') return null;
    const raw = sessionStorage.getItem(key);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as T;
    } catch {
      sessionStorage.removeItem(key);
      return null;
    }
  }

  private remove(key: string): void {
    if (typeof sessionStorage === 'undefined') return;
    sessionStorage.removeItem(key);
  }
}
