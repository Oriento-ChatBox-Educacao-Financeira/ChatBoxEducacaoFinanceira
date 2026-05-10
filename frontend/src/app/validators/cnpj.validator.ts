import { AbstractControl, ValidationErrors } from '@angular/forms';

export function validarCNPJ(valor: string): boolean {
  const cnpj = (valor || '').replace(/\D/g, '');

  if (cnpj.length !== 14) return false;
  if (/^(\d)\1{13}$/.test(cnpj)) return false;

  const calcularDigito = (base: string, pesos: number[]): number => {
    const soma = base.split('').reduce((acc, num, i) => acc + parseInt(num, 10) * pesos[i], 0);
    const resto = soma % 11;
    return resto < 2 ? 0 : 11 - resto;
  };

  const pesos1 = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  const pesos2 = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];

  const digito1 = calcularDigito(cnpj.substring(0, 12), pesos1);
  if (digito1 !== parseInt(cnpj[12], 10)) return false;

  const digito2 = calcularDigito(cnpj.substring(0, 13), pesos2);
  if (digito2 !== parseInt(cnpj[13], 10)) return false;

  return true;
}

export function cnpjValidator(control: AbstractControl): ValidationErrors | null {
  const valor: string = control.value || '';
  if (!valor) return null;
  return validarCNPJ(valor) ? null : { cnpjInvalido: true };
}
