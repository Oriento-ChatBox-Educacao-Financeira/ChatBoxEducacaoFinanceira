import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private auth: AuthService) {}

  /**
   * Anexa o JWT do Supabase como Authorization: Bearer em todas as requisições
   * que tenham token disponível. A renovação é gerenciada pelo supabase-js.
   *
   * Nota: o backend Spring precisa ser ajustado para validar o JWT do Supabase
   * (via JWKS/issuer) — caso contrário, endpoints protegidos vão retornar 401
   * mesmo com o header presente. Acompanhar essa migração em issue dedicada.
   */
  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.auth.getAccessToken();
    if (!token) {
      return next.handle(req);
    }
    return next.handle(
      req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    );
  }
}
