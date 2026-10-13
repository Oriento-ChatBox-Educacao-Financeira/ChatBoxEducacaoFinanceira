import { Injectable } from '@angular/core';
import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
} from '@angular/common/http';
import { Observable, catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { environment } from '../../environments/environment';

const SKIP_AUTH_PATHS = ['/api/auth/login', '/api/auth/refresh', '/api/auth/cadastro', '/api/auth/register'];

/**
 * Injeta o JWT do backend Spring em todas as requisi\u00e7\u00f5es para a API.
 * Em caso de 401, tenta um refresh \u00fanico antes de propagar o erro.
 */
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private refreshing = false;

  constructor(private auth: AuthService) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const isApi = req.url.startsWith(environment.apiUrl) || req.url.startsWith(environment.authUrl);
    const shouldSkip = SKIP_AUTH_PATHS.some((path) => req.url.endsWith(path));

    if (!isApi || shouldSkip) {
      return next.handle(req);
    }

    const token = this.auth.getAccessToken();
    const cloned = token
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

    return next.handle(cloned).pipe(
      catchError((err: unknown) => {
        if (
          err instanceof HttpErrorResponse &&
          err.status === 401 &&
          !this.refreshing &&
          this.auth.getRefreshToken()
        ) {
          this.refreshing = true;
          return this.auth.refreshAccessToken().pipe(
            switchMap((newToken) => {
              this.refreshing = false;
              const retried = req.clone({
                setHeaders: { Authorization: `Bearer ${newToken}` },
              });
              return next.handle(retried);
            }),
            catchError((refreshErr) => {
              this.refreshing = false;
              return throwError(() => refreshErr);
            }),
          );
        }
        return throwError(() => err);
      }),
    );
  }
}
