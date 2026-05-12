import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { ErrorHandlerService } from '../../services/error-handler.service';
import { LoggerService } from '../../services/logger.service';
import { RegisterStateService } from '../../services/register-state.service';

@Component({
  selector: 'app-register-confirm',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './register-confirm.html',
  styleUrls: ['./register-confirm.css'],
})
export class RegisterConfirm implements OnDestroy {
  confirmForm: FormGroup;
  loading = false;
  errorMessage = '';
  passwordsMatch = false;

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private errorHandler: ErrorHandlerService,
    private logger: LoggerService,
    private registerState: RegisterStateService,
  ) {
    this.confirmForm = this.fb.group({
      confirmarEmail: ['', [Validators.required, Validators.email]],
      confirmarSenha: ['', [Validators.required]],
    });
  }

  // bloqueia acesso direto a página de confirmação sem passar pelo registro (comentado para facilitar testes)
  // ngOnInit(): void {
  //   if (!this.registerState.getData()) {
  //     this.router.navigate(['/register']);
  //   }
  // }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  checkPasswordMatch(): void {
    const senhaOriginal = this.registerState.getData()?.senha ?? '';
    const confirmarSenha = this.confirmForm.get('confirmarSenha')?.value ?? '';
    this.passwordsMatch = senhaOriginal === confirmarSenha;
  }

  onSubmit(): void {
    if (this.confirmForm.invalid || !this.passwordsMatch) {
      this.confirmForm.markAllAsTouched();
      return;
    }

    const dadosPasso1 = this.registerState.getData();
    if (!dadosPasso1) {
      this.router.navigate(['/register']);
      return;
    }

    if (dadosPasso1.email !== this.confirmForm.value.confirmarEmail) {
      this.confirmForm.get('confirmarEmail')?.setErrors({ emailNaoConfere: true });
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.authService
      .register({
        nome: dadosPasso1.nomeFantasia,
        nomeFantasia: dadosPasso1.nomeFantasia,
        email: dadosPasso1.email,
        senha: dadosPasso1.senha,
        cnpj: dadosPasso1.cnpj,
        razaoSocial: dadosPasso1.razaoSocial || undefined,
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loading = false;
          this.registerState.clear();
          this.logger.log('Cadastro finalizado, redirecionando para login');
          this.router.navigate(['/login']);
        },
        error: (err) => {
          this.loading = false;
          this.errorMessage = this.errorHandler.handleAuthError(err);
          this.logger.error('Erro ao finalizar cadastro', err);
        },
      });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
