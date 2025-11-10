import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { PrimaryButton } from '../../_components/primary-button/primary-button';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, PrimaryButton],
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
})
export class Login {
  loginForm: FormGroup;
  loading = false;
  errorMessage = '';

  constructor(private fb: FormBuilder, private router: Router, private authService: AuthService) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, this.emailOuCnpjValidator]],
      senha: ['', [Validators.required, Validators.minLength(6)]],
    });
  }

  emailOuCnpjValidator(control: any) {
    const value = control.value;

    // Regex de e-mail
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    // Regex de CNPJ
    const cnpjRegex = /^\d{14}$/;

    if (!value) return null;
    if (emailRegex.test(value) || cnpjRegex.test(value)) return null;

    return { emailOuCnpjInvalido: true };
  }

  onSubmit() {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    const { email, senha } = this.loginForm.value;

    this.authService.login(email, senha).subscribe({
      next: (res) => {
        console.log('Login bem-sucedido:', res);
        this.loading = false;
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        console.error('Erro ao logar:', err);
        this.errorMessage =
          err.status === 401
            ? 'E-mail ou senha inválidos.'
            : 'Erro inesperado ao tentar fazer login.';
      },
    });
  }

  goToRegister() {
    this.router.navigate(['/register']);
  }

  getEmailErrorMessage(): string {
    const email = this.loginForm.get('email');
    if (email?.hasError('required')) return 'O e-mail ou CNPJ é obrigatório.';
    if (email?.hasError('emailOuCnpjInvalido')) return 'Digite um e-mail ou CNPJ válido.';
    return '';
  }

  getSenhaErrorMessage(): string {
    const senha = this.loginForm.get('senha');
    if (senha?.hasError('required')) return 'A senha é obrigatória.';
    if (senha?.hasError('minlength')) return 'A senha deve ter no mínimo 6 caracteres.';
    return '';
  }
}
