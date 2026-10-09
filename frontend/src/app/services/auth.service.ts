import { Injectable, Injector } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import {
  BehaviorSubject,
  Observable,
  catchError,
  map,
  of,
  switchMap,
  tap,
  throwError,
} from 'rxjs';

import { environment } from '../../environments/environment';
import { LoggerService } from './logger.service';
import { ContextoFinanceiroService } from './contexto-financeiro.service';
import { LoginResponse, Usuario } from '../models/usuario.model';

const ACCESS_TOKEN_KEY = 'oriento.accessToken';
const REFRESH_TOKEN_KEY = 'oriento.refreshToken';
const USER_KEY = 'oriento.user';

export interface SignUpInput {
  nome: string;
  nomeFantasia: string;
  cnpj: string;
  razaoSocial?: string;
  email: string;
  senha: string;
}

interface BackendUsuarioResponse {
  id: string;
  nome: string;
  email: string;
  empresa: {
    id: number;
    cnpj: string;
    nomeFantasia: string | null;
    razaoSocial: string | null;
  } | null;
}

interface BackendLoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  usuario: BackendUsuarioResponse;
}

export class EmailAlreadyRegisteredError extends Error {
  constructor() {
    super('E-mail já cadastrado.');
    this.name = 'EmailAlreadyRegisteredError';
  }
}

/**
 * Servi\u00e7o de autentica\u00e7\u00e3o que conversa exclusivamente com o backend
 * Spring (/api/auth). Tokens e snapshot do usu\u00e1rio s\u00e3o persistidos no
 * localStorage. O componente raiz e o {@code AuthGuard} consomem
 * {@code user$} e {@code isAuthenticated()} para tomar decis\u00f5es de UI.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly authBase = environment.authUrl;

  private accessToken: string | null = null;
  private refreshToken: string | null = null;

  private userSubject = new BehaviorSubject<Usuario | null>(null);
  user$: Observable<Usuario | null> = this.userSubject.asObservable();

  private authReadySubject = new BehaviorSubject<boolean>(false);
  authReady$: Observable<boolean> = this.authReadySubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router,
    private logger: LoggerService,
    private injector: Injector,
  ) {
    this.bootstrap();
  }

  whenReady(): Promise<void> {
    if (this.authReadySubject.value) return Promise.resolve();
    return new Promise<void>((resolve) => {
      const sub = this.authReadySubject.subscribe((ready) => {
        if (ready) {
          resolve();
          queueMicrotask(() => sub.unsubscribe());
        }
      });
    });
  }

  /**
   * L\u00ea o estado persistido em localStorage e tenta hidratar o usu\u00e1rio.
   * Se o token estiver presente mas o GET /me falhar, derruba a sess\u00e3o
   * para evitar inconsist\u00eancia.
   */
  private bootstrap(): void {
    if (typeof localStorage === 'undefined') {
      this.authReadySubject.next(true);
      return;
    }
    this.accessToken = localStorage.getItem(ACCESS_TOKEN_KEY);
    this.refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
    const cachedUser = localStorage.getItem(USER_KEY);
    if (cachedUser) {
      try {
        this.userSubject.next(JSON.parse(cachedUser));
      } catch {
        localStorage.removeItem(USER_KEY);
      }
    }

    if (!this.accessToken) {
      this.authReadySubject.next(true);
      return;
    }

    this.http
      .get<BackendUsuarioResponse>(`${this.authBase}/me`)
      .subscribe({
        next: (resp) => {
          this.persistUser(resp);
          this.authReadySubject.next(true);
        },
        error: (err) => {
          this.logger.warn?.('Falha ao hidratar /me; derrubando sess\u00e3o', err);
          this.clearTokens();
          this.limparCachesAuxiliares();
          this.userSubject.next(null);
          this.authReadySubject.next(true);
        },
      });
  }

  register(input: SignUpInput): Observable<void> {
    const payload = {
      nome: input.nome,
      email: input.email,
      senha: input.senha,
      cnpj: input.cnpj.replace(/\D/g, ''),
      nomeFantasia: input.nomeFantasia,
      razaoSocial: input.razaoSocial?.trim() ? input.razaoSocial.trim() : null,
    };
    return this.http
      .post<{ message: string }>(`${this.authBase}/cadastro`, payload, {
        observe: 'response',
      })
      .pipe(
        map(() => void 0),
        catchError((err) => {
          if (err?.status === 409) {
            return throwError(() => new EmailAlreadyRegisteredError());
          }
          return throwError(() => err);
        }),
      );
  }

  login(emailOrCnpj: string, senha: string): Observable<Usuario> {
    const body = emailOrCnpj.includes('@')
      ? { email: emailOrCnpj, senha }
      : { cnpj: emailOrCnpj.replace(/\D/g, ''), senha };
    return this.http.post<BackendLoginResponse>(`${this.authBase}/login`, body).pipe(
      tap((resp) => this.persistSession(resp)),
      map((resp) => this.toUsuario(resp.usuario)),
    );
  }

  logout(): Observable<void> {
    const refresh = this.refreshToken;
    const end$ = refresh
      ? this.http.post<void>(`${this.authBase}/logout`, { refreshToken: refresh }).pipe(
          catchError(() => of(void 0)),
        )
      : of(void 0);

    return end$.pipe(
      tap(() => {
        this.clearTokens();
        this.limparCachesAuxiliares();
        this.userSubject.next(null);
        this.router.navigate(['/login']);
      }),
      map(() => void 0),
    );
  }

  private limparCachesAuxiliares(): void {
    try {
      this.injector.get(ContextoFinanceiroService).limpar();
    } catch (err) {
      this.logger.warn?.('Falha ao limpar cache de contexto financeiro no logout', err);
    }
  }

  /**
   * Tenta renovar o access token usando o refresh em mem\u00f3ria. \u00c9 chamado
   * pelo {@code AuthInterceptor} quando uma resposta retorna 401.
   */
  refreshAccessToken(): Observable<string> {
    if (!this.refreshToken) {
      return throwError(() => new Error('No refresh token'));
    }
    return this.http
      .post<BackendLoginResponse>(`${this.authBase}/refresh`, {
        refreshToken: this.refreshToken,
      })
      .pipe(
        tap((resp) => this.persistSession(resp)),
        map((resp) => resp.accessToken),
      );
  }

  trocarSenha(novaSenha: string): Observable<void> {
    return this.http
      .patch<void>(`${this.authBase}/senha`, { novaSenha })
      .pipe(map(() => void 0));
  }

  /**
   * Carrega o perfil atual a partir do backend (n\u00e3o usa cache).
   * \u00datil para refletir mudan\u00e7as de empresa/perfil ap\u00f3s edi\u00e7\u00e3o.
   */
  refreshCurrentUser(): Observable<Usuario | null> {
    if (!this.accessToken) {
      return of(null);
    }
    return this.http.get<BackendUsuarioResponse>(`${this.authBase}/me`).pipe(
      tap((resp) => this.persistUser(resp)),
      map(() => this.userSubject.value),
    );
  }

  getAccessToken(): string | null {
    return this.accessToken;
  }

  getRefreshToken(): string | null {
    return this.refreshToken;
  }

  isAuthenticated(): boolean {
    return !!this.accessToken;
  }

  getCurrentUser(): Usuario | null {
    return this.userSubject.value;
  }

  // ============================================================
  // Internos
  // ============================================================

  private persistSession(resp: BackendLoginResponse): void {
    this.accessToken = resp.accessToken;
    this.refreshToken = resp.refreshToken;
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(ACCESS_TOKEN_KEY, resp.accessToken);
      localStorage.setItem(REFRESH_TOKEN_KEY, resp.refreshToken);
    }
    this.persistUser(resp.usuario);
  }

  private persistUser(resp: BackendUsuarioResponse): void {
    const usuario = this.toUsuario(resp);
    this.userSubject.next(usuario);
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(USER_KEY, JSON.stringify(usuario));
    }
  }

  private toUsuario(resp: BackendUsuarioResponse): Usuario {
    return {
      id: resp.id,
      nome: resp.nome,
      email: resp.email,
      cnpj: resp.empresa?.cnpj ?? '',
      nomeFantasia: resp.empresa?.nomeFantasia ?? undefined,
      razaoSocial: resp.empresa?.razaoSocial ?? undefined,
    };
  }

  private clearTokens(): void {
    this.accessToken = null;
    this.refreshToken = null;
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem(ACCESS_TOKEN_KEY);
      localStorage.removeItem(REFRESH_TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    }
  }
}

// Mant\u00e9m o LoginResponse tipo p\u00fablico para componentes que importam.
export type { LoginResponse };
