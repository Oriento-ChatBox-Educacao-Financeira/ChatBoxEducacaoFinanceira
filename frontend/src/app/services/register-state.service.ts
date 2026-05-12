import { Injectable } from '@angular/core';

export interface RegisterData {
  nomeFantasia: string;
  razaoSocial?: string;
  cnpj: string;
  email: string;
  senha: string;
}

@Injectable({ providedIn: 'root' })
export class RegisterStateService {
  private data: RegisterData | null = null;

  setData(data: RegisterData): void {
    this.data = data;
  }

  getData(): RegisterData | null {
    return this.data;
  }

  clear(): void {
    this.data = null;
  }
}
