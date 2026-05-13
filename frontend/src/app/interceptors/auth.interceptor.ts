import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { environment } from '../../environments/environment';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private auth: AuthService) {}

  /**
   * Intercepta requisições HTTP e adiciona o token de autenticação do Supabase
   * (Authorization: Bearer ...) apenas para chamadas ao próprio Supabase.
   *
   * O backend Spring atual valida JWT assinado com RSA via
   * `NimbusJwtDecoder.withPublicKey(...)` e não aceita o JWT HS256 do Supabase;
   * por isso o token NÃO é anexado às requisições para `environment.apiUrl`,
   * evitando 401 nos endpoints do backend (ex.: /api/oriento/ask).
   *
   * Quando o backend for ajustado para aceitar JWKS do Supabase (ou um exchange
   * token for introduzido), este filtro de URL pode ser removido.
   */
  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.auth.getAccessToken();
    if (!token || !this.isSupabaseRequest(req.url)) {
      return next.handle(req);
    }
    return next.handle(
      req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    );
  }

  private isSupabaseRequest(url: string): boolean {
    try {
      const target = new URL(url, window.location.origin);
      const supabaseHost = new URL(environment.supabase.url).host;
      return target.host === supabaseHost;
    } catch {
      return false;
    }
  }
}
