import { Component } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule,
  AbstractControl,
} from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { PrimaryButton } from '../../_components/primary-button/primary-button';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, PrimaryButton],
  templateUrl: './register.html',
  styleUrls: ['./register.css'],
})
export class Register {
  registerForm: FormGroup;
  loading = false;
  errorMessage = '';

  constructor(private fb: FormBuilder, private router: Router, private authService: AuthService) {
    this.registerForm = this.fb.group(
      {
        nomeFantasia: ['', [Validators.required, Validators.maxLength(150)]],
        razaoSocial: [''],
        cnpj: ['', [Validators.required, Validators.minLength(14)]],
        email: ['', [Validators.required, Validators.email]],
        confirmarEmail: ['', [Validators.required, Validators.email]],
        senha: ['', [Validators.required, Validators.minLength(6)]],
        confirmarSenha: ['', [Validators.required]],
      },
      {
        validators: [this.emailsIguais, this.senhasIguais],
      }
    );
  }

  // Validação: e-mails iguais
  private emailsIguais(control: AbstractControl) {
    const email = control.get('email')?.value;
    const confirmarEmail = control.get('confirmarEmail')?.value;
    return email === confirmarEmail ? null : { emailMismatch: true };
  }

  // Validação: senhas iguais
  private senhasIguais(control: AbstractControl) {
    const senha = control.get('senha')?.value;
    const confirmarSenha = control.get('confirmarSenha')?.value;
    return senha === confirmarSenha ? null : { passwordMismatch: true };
  }

  onSubmit() {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    const { nomeFantasia, email, senha, cnpj } = this.registerForm.value;

    this.authService.register({ nome: nomeFantasia, email, senha, cnpj }).subscribe({
      next: (res) => {
        console.log('Cadastro realizado com sucesso:', res);
        this.loading = false;
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.loading = false;
        console.error('Erro ao cadastrar:', err);
        this.errorMessage =
          err.status === 400
            ? 'Dados inválidos. Verifique as informações.'
            : 'Erro inesperado ao tentar cadastrar.';
      },
    });
  }

  goToLogin() {
    this.router.navigate(['/login']);
  }

  // mensagens de erro
  getErrorMessage(field: string): string {
    const control = this.registerForm.get(field);
    if (control?.hasError('required')) return 'Campo obrigatório.';
    if (control?.hasError('email')) return 'Digite um e-mail válido.';
    if (control?.hasError('minlength')) return 'Quantidade mínima de caracteres não atingida.';
    if (control?.hasError('maxlength')) return 'Campo excedeu o limite de caracteres.';
    return '';
  }
}
