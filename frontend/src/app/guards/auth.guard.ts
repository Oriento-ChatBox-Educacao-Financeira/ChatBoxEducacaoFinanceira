import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}

  /**
   * Aguarda a hidratação inicial da sessão (bootstrapSession) antes de
   * decidir, para não redirecionar a /login durante o boot quando o
   * usuário tem sessão Supabase válida em localStorage.
   */
  async canActivate(): Promise<boolean | UrlTree> {
    await this.auth.whenReady();
    if (this.auth.isAuthenticated()) {
      return true;
    }
    return this.router.createUrlTree(['/login']);
  }
}
