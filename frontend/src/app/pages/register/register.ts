import { Component, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Subject } from 'rxjs';
import { CustomValidators } from '../../validators/custom-validators';
import { RegisterStateService } from '../../services/register-state.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './register.html',
  styleUrls: ['./register.css'],
})
export class Register implements OnDestroy {
  registerForm: FormGroup;

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private registerState: RegisterStateService,
  ) {
    this.registerForm = this.fb.group({
      nome: ['', [Validators.required, Validators.maxLength(150)]],
      nomeFantasia: ['', [Validators.required, Validators.maxLength(150)]],
      cnpj: ['', [Validators.required, CustomValidators.cnpj]],
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
