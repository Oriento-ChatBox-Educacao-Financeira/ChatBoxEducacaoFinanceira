import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { CustomValidators } from '../../validators/custom-validators';
import { RegisterStateService } from '../../services/register-state.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './register.html',
  styleUrls: ['./register.css'],
})
export class Register {
  registerForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private registerState: RegisterStateService,
  ) {
    const saved = this.registerState.getIdentity();
    this.registerForm = this.fb.group({
      nome: [saved?.nome ?? '', [Validators.required, Validators.maxLength(150)]],
      nomeFantasia: [
        saved?.nomeFantasia ?? '',
        [Validators.required, Validators.maxLength(150)],
      ],
      cnpj: [saved?.cnpj ?? '', [Validators.required, CustomValidators.cnpj]],
    });
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    const { nome, nomeFantasia, cnpj } = this.registerForm.value;
    this.registerState.setIdentity({ nome, nomeFantasia, cnpj });
    this.router.navigate(['/register/credenciais']);
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  getErrorMessage(field: string): string {
    const control = this.registerForm.get(field);
    if (!control) return '';

    if (control.hasError('required')) return 'Campo obrigatório.';
    if (control.hasError('cnpjInvalido')) return 'CNPJ inválido. Verifique os dígitos.';

    if (control.hasError('maxlength')) {
      const max = control.errors?.['maxlength'].requiredLength;
      return `Máximo de ${max} caracteres.`;
    }

    return '';
  }
}
