import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { ErrorHandlerService } from '../../services/error-handler.service';
import { LoggerService } from '../../services/logger.service';
import { CustomValidators } from '../../validators/custom-validators';
import { RegisterStateService } from '../../services/register-state.service';

@Component({
  selector: 'app-register-confirm',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './register-confirm.html',
  styleUrls: ['./register-confirm.css'],
})
export class RegisterConfirm implements OnInit, OnDestroy {
  credentialsForm: FormGroup;
  loading = false;
  errorMessage = '';
  passwordStrength = 0;
  strengthLabel = '';
  strengthColor = '';
  passwordsMatch = false;
  emailsMatch = false;
  showSenha = false;
  showConfirmarSenha = false;
  passwordCriteria = {
    tamanho: false,
    maiuscula: false,
    minuscula: false,
    numero: false,
    especial: false,
  };

  toggleSenha(): void {
    this.showSenha = !this.showSenha;
  }

  toggleConfirmarSenha(): void {
    this.showConfirmarSenha = !this.showConfirmarSenha;
  }

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private errorHandler: ErrorHandlerService,
    private logger: LoggerService,
    private registerState: RegisterStateService,
  ) {
    const saved = this.registerState.getCredentials();
    this.credentialsForm = this.fb.group({
      email: [saved?.email ?? '', [Validators.required, Validators.email]],
      confirmarEmail: [saved?.confirmarEmail ?? '', [Validators.required, Validators.email]],
      senha: [saved?.senha ?? '', [Validators.required, CustomValidators.senhaForte]],
      confirmarSenha: [saved?.confirmarSenha ?? '', [Validators.required]],
      aceiteTermos: [saved?.aceiteTermos ?? false, Validators.requiredTrue],
    });
  }

  ngOnInit(): void {
    if (!this.registerState.getIdentity()) {
      this.router.navigate(['/register']);
      return;
    }
    if (this.credentialsForm.get('senha')?.value) {
      const event = new Event('input');
      Object.defineProperty(event, 'target', {
        value: { value: this.credentialsForm.get('senha')?.value },
      });
      this.updatePasswordStrength(event);
    }
    this.checkEmailMatch();
  }

  private persistCredentialsDraft(): void {
    this.registerState.setCredentials(this.credentialsForm.getRawValue());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  updatePasswordStrength(event: Event): void {
    const valor = (event.target as HTMLInputElement).value;

    this.passwordCriteria = {
      tamanho: valor.length >= 8,
      maiuscula: /[A-Z]/.test(valor),
      minuscula: /[a-z]/.test(valor),
      numero: /[0-9]/.test(valor),
      especial: /[!@#$%^&*(),.?":{}|<>]/.test(valor),
    };

    if (!valor) {
      this.passwordStrength = 0;
      this.strengthLabel = '';
      this.strengthColor = '';
      this.checkPasswordMatch();
      return;
    }

    let score = 0;
    if (this.passwordCriteria.tamanho) score++;
    if (this.passwordCriteria.maiuscula) score++;
    if (this.passwordCriteria.minuscula) score++;
    if (this.passwordCriteria.numero) score++;
    if (this.passwordCriteria.especial) score++;

    this.passwordStrength = score <= 1 ? 1 : score === 2 ? 2 : score <= 4 ? 3 : 4;

    const map: Record<number, { label: string; color: string }> = {
      1: { label: 'fraca', color: 'weak' },
      2: { label: 'razoável', color: 'fair' },
      3: { label: 'boa', color: 'good' },
      4: { label: 'forte', color: 'strong' },
    };

    this.strengthLabel = map[this.passwordStrength]?.label ?? '';
    this.strengthColor = map[this.passwordStrength]?.color ?? '';

    this.checkPasswordMatch();
  }

  checkPasswordMatch(): void {
    const senha = this.credentialsForm.get('senha')?.value ?? '';
    const confirmar = this.credentialsForm.get('confirmarSenha')?.value ?? '';
    this.passwordsMatch = !!senha && senha === confirmar;
  }

  checkEmailMatch(): void {
    const emailCtrl = this.credentialsForm.get('email');
    const confirmarCtrl = this.credentialsForm.get('confirmarEmail');
    const email = emailCtrl?.value ?? '';
    const confirmar = confirmarCtrl?.value ?? '';
    this.emailsMatch = !!email && email === confirmar;

    if (this.emailsMatch && confirmarCtrl?.hasError('emailNaoConfere')) {
      const { emailNaoConfere: _drop, ...rest } = confirmarCtrl.errors ?? {};
      confirmarCtrl.setErrors(Object.keys(rest).length ? rest : null);
    }
  }

  onSubmit(): void {
    this.checkEmailMatch();
    this.checkPasswordMatch();

    if (this.credentialsForm.invalid || !this.passwordsMatch || !this.emailsMatch) {
      this.credentialsForm.markAllAsTouched();
      if (!this.emailsMatch) {
        this.credentialsForm.get('confirmarEmail')?.setErrors({ emailNaoConfere: true });
      }
      return;
    }

    const identity = this.registerState.getIdentity();
    if (!identity) {
      this.router.navigate(['/register']);
      return;
    }

    const { email, senha } = this.credentialsForm.value;

    this.loading = true;
    this.errorMessage = '';

    this.authService
      .register({
        nome: identity.nome,
        nomeFantasia: identity.nomeFantasia,
        cnpj: identity.cnpj,
        email,
        senha,
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loading = false;
          this.registerState.clear();
          this.logger.log('Cadastro criado, indo para tela de aviso');
          this.router.navigate(['/register/sucesso'], { queryParams: { email } });
        },
        error: (err) => {
          this.loading = false;
          this.errorMessage = this.errorHandler.handleAuthError(err);
          this.logger.error('Erro ao criar cadastro', err);
        },
      });
  }

  goToLogin(): void {
    this.persistCredentialsDraft();
    this.router.navigate(['/login']);
  }

  goBack(): void {
    this.persistCredentialsDraft();
    this.router.navigate(['/register']);
  }

  getErrorMessage(field: string): string {
    const control = this.credentialsForm.get(field);
    if (!control) return '';

    if (control.hasError('required')) return 'Campo obrigatório.';
    if (control.hasError('email')) return 'Digite um e-mail válido.';
    if (control.hasError('emailNaoConfere')) return 'Os e-mails não coincidem.';

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
