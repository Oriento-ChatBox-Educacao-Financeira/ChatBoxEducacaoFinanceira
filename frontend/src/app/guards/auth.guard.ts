import { Injectable } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({ providedIn: 'root' })
export class AuthGuard {
  constructor(private auth: AuthService, private router: Router) {}

  // Verifica se o usuário pode acessar a rota
  canActivate(): boolean {
    if (this.auth.isAuthenticated()) return true;

    // Se não estiver logado, redireciona para o login
    this.router.navigate(['/login']);
    return false;
  }
}
