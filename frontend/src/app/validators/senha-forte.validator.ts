import { AbstractControl, ValidationErrors } from '@angular/forms';

export interface SenhaErros {
  tamanhoMinimo?: boolean;
  semMaiuscula?: boolean;
  semMinuscula?: boolean;
  semNumero?: boolean;
  semEspecial?: boolean;
}

export function senhaForte(control: AbstractControl): ValidationErrors | null {
  const valor: string = control.value || '';
  if (!valor) return null;

  const erros: SenhaErros = {};

  if (valor.length < 8) erros.tamanhoMinimo = true;
  if (!/[A-Z]/.test(valor)) erros.semMaiuscula = true;
  if (!/[a-z]/.test(valor)) erros.semMinuscula = true;
  if (!/[0-9]/.test(valor)) erros.semNumero = true;
  if (!/[!@#$%^&*()\-_=+\[\]{};':",./<>?|\\]/.test(valor)) erros.semEspecial = true;

  return Object.keys(erros).length > 0 ? erros : null;
}
