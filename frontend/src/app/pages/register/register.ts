import { Component, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Subject } from 'rxjs';
import { PrimaryButton } from '../../_components/primary-button/primary-button';
import { ErrorHandlerService } from '../../services/error-handler.service';
import { LoggerService } from '../../services/logger.service';
import { CustomValidators } from '../../validators/custom-validators';
import { RegisterStateService } from '../../services/register-state.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, PrimaryButton],
  templateUrl: './register.html',
  styleUrls: ['./register.css'],
})
export class Register implements OnDestroy {
  registerForm: FormGroup;
  loading = false;
  errorMessage = '';
  passwordStrength = 0;
  strengthLabel = '';
  strengthColor = '';

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private errorHandler: ErrorHandlerService,
    private logger: LoggerService,
    private registerState: RegisterStateService,
  ) {
    this.registerForm = this.fb.group({
      nomeFantasia: ['', [Validators.required, Validators.maxLength(150)]],
      razaoSocial: [''],
      cnpj: ['', [Validators.required, CustomValidators.cnpj]],
      email: ['', [Validators.required, Validators.email]],
      senha: ['', [Validators.required, CustomValidators.senhaForte]],
      aceiteTermos: [false, Validators.requiredTrue],
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    const { nomeFantasia, razaoSocial, cnpj, email, senha } = this.registerForm.value;
    this.registerState.setData({ nomeFantasia, razaoSocial, cnpj, email, senha });
    this.router.navigate(['/register/confirm']);
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  updatePasswordStrength(event: Event): void {
    const valor = (event.target as HTMLInputElement).value;

    if (!valor) {
      this.passwordStrength = 0;
      this.strengthLabel = '';
      this.strengthColor = '';
      return;
    }

    let score = 0;
    if (valor.length >= 8) score++;
    if (/[A-Z]/.test(valor)) score++;
    if (/[a-z]/.test(valor)) score++;
    if (/[0-9]/.test(valor)) score++;
    if (/[!@#$%^&*(),.?":{}|<>]/.test(valor)) score++;

    this.passwordStrength = score <= 1 ? 1 : score === 2 ? 2 : score <= 4 ? 3 : 4;

    const map: Record<number, { label: string; color: string }> = {
      1: { label: 'FRACA', color: 'weak' },
      2: { label: 'RAZOÁVEL', color: 'fair' },
      3: { label: 'CONFIÁVEL', color: 'good' },
      4: { label: 'RESISTENTE', color: 'strong' },
    };

    this.strengthLabel = map[this.passwordStrength]?.label ?? '';
    this.strengthColor = map[this.passwordStrength]?.color ?? '';
  }

  getErrorMessage(field: string): string {
    const control = this.registerForm.get(field);
    if (!control) return '';

    if (control.hasError('required')) return 'Campo obrigatório.';
    if (control.hasError('email')) return 'Digite um e-mail válido.';
    if (control.hasError('cnpjInvalido')) return 'CNPJ inválido. Verifique os dígitos.';

    if (control.hasError('maxlength')) {
      const max = control.errors?.['maxlength'].requiredLength;
      return `Máximo de ${max} caracteres.`;
    }

    if (control.hasError('senhaFraca')) {
      const erros = control.errors?.['senhaFraca'];
      if (erros?.tamanhoMinimo) return 'Mínimo de 8 caracteres.';
      if (erros?.semMaiuscula) return 'Inclua pelo menos uma letra maiúscula.';
      if (erros?.semMinuscula) return 'Inclua pelo menos uma letra minúscula.';
      if (erros?.semNumero) return 'Inclua pelo menos um número.';
      if (erros?.semEspecial) return 'Inclua pelo menos um caractere especial.';
    }

    return '';
  }
}
