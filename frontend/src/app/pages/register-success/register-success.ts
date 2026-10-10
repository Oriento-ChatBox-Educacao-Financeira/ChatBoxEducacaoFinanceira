import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { SupabaseClientService } from '../../services/supabase.client';
import { ErrorHandlerService } from '../../services/error-handler.service';
import { LoggerService } from '../../services/logger.service';

const RESEND_COOLDOWN_SECONDS = 60;

@Component({
  selector: 'app-register-success',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './register-success.html',
  styleUrls: ['./register-success.css'],
})
export class RegisterSuccess implements OnInit, OnDestroy {
  email = '';
  resending = false;
  resendMessage = '';
  resendError = '';
  resendCooldown = 0;
  private cooldownHandle: ReturnType<typeof setInterval> | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private supabase: SupabaseClientService,
    private errorHandler: ErrorHandlerService,
    private logger: LoggerService,
  ) {}

  ngOnInit(): void {
    this.email = this.route.snapshot.queryParamMap.get('email') ?? '';
  }

  ngOnDestroy(): void {
    this.clearCooldown();
  }

  async resendEmail(): Promise<void> {
    if (!this.email || this.resending || this.resendCooldown > 0) return;

    this.resending = true;
    this.resendMessage = '';
    this.resendError = '';

    try {
      const { error } = await this.supabase.client.auth.resend({
        type: 'signup',
        email: this.email,
      });
      if (error) throw error;
      this.resendMessage = 'E-mail de confirmação reenviado. Cheque sua caixa de entrada.';
      this.startCooldown();
      this.logger.log('E-mail de confirmação reenviado', { email: this.email });
    } catch (err) {
      this.resendError = this.errorHandler.handleAuthError(err as Error);
      this.logger.error('Falha ao reenviar e-mail de confirmação', err);
    } finally {
      this.resending = false;
    }
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  private startCooldown(): void {
    this.clearCooldown();
    this.resendCooldown = RESEND_COOLDOWN_SECONDS;
    this.cooldownHandle = setInterval(() => {
      this.resendCooldown -= 1;
      if (this.resendCooldown <= 0) {
        this.clearCooldown();
      }
    }, 1000);
  }

  private clearCooldown(): void {
    if (this.cooldownHandle) {
      clearInterval(this.cooldownHandle);
      this.cooldownHandle = null;
    }
  }
}
