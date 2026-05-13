import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, from, throwError } from 'rxjs';
import { map, tap, catchError, switchMap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { Usuario } from '../models/usuario.model';
import { LoggerService } from './logger.service';
import { SupabaseClientService } from './supabase.client';

export interface SignUpInput {
  nome: string;
  nomeFantasia: string;
  cnpj: string;
  razaoSocial?: string;
  email: string;
  senha: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private userSubject = new BehaviorSubject<Usuario | null>(null);
  private accessToken: string | null = null;

  user$: Observable<Usuario | null> = this.userSubject.asObservable();

  constructor(
    private supabase: SupabaseClientService,
    private router: Router,
    private logger: LoggerService
  ) {
    this.bootstrapSession();
    this.listenAuthChanges();
  }

  /**
   * Restaura a sessão persistida pelo supabase-js (localStorage) ao iniciar.
   */
  private async bootstrapSession(): Promise<void> {
    const { data } = await this.supabase.client.auth.getSession();
    if (data.session) {
      this.accessToken = data.session.access_token;
      await this.hydrateUsuario(data.session.user.id);
    }
  }

  /**
   * Escuta eventos do supabase-js e mantém o token e o usuário em memória.
   */
  private listenAuthChanges(): void {
    this.supabase.client.auth.onAuthStateChange((_event, session) => {
      this.accessToken = session?.access_token ?? null;
      if (!session) {
        this.userSubject.next(null);
        return;
      }
      this.hydrateUsuario(session.user.id).catch((err) =>
        this.logger.warn('Falha ao hidratar usuário após auth change', err)
      );
    });
  }

  /**
   * Carrega o perfil em public.usuario e emite no userSubject.
   */
  private async hydrateUsuario(authUserId: string): Promise<void> {
    const { data, error } = await this.supabase.client
      .from('usuario')
      .select('id_usuario,nome,email')
      .eq('id_usuario', authUserId)
      .maybeSingle();

    if (error) {
      this.logger.warn('Erro ao buscar perfil do usuário', error);
      return;
    }

    if (data) {
      this.userSubject.next({
        id: data.id_usuario,
        nome: data.nome,
        email: data.email,
        cnpj: '',
      });
    }
  }

  /**
   * Registra um novo usuário no Supabase Auth.
   * As linhas em public.usuario e public.empresa são criadas pela trigger
   * handle_new_user a partir do raw_user_meta_data.
   */
  register(input: SignUpInput): Observable<void> {
    return from(
      this.supabase.client.auth.signUp({
        email: input.email,
        password: input.senha,
        options: {
          data: {
            nome: input.nome,
            nome_fantasia: input.nomeFantasia,
            cnpj: input.cnpj,
            razao_social: input.razaoSocial ?? null,
          },
        },
      })
    ).pipe(
      map(({ data, error }) => {
        if (error) throw error;
        if (!data.user) throw new Error('Falha ao criar usuário.');
      }),
      tap(() => this.logger.log('Cadastro criado no Supabase')),
      catchError((err) => {
        this.logger.error('Erro no cadastro Supabase', err);
        return throwError(() => err);
      })
    );
  }

  /**
   * Realiza o login e atualiza o usuário em memória.
   */
  login(email: string, senha: string): Observable<Usuario> {
    return from(
      this.supabase.client.auth.signInWithPassword({ email, password: senha })
    ).pipe(
      switchMap(({ data, error }) => {
        if (error) return throwError(() => error);
        if (!data.user) return throwError(() => new Error('Sessão inválida.'));
        return from(this.hydrateUsuario(data.user.id)).pipe(
          map(
            () =>
              this.userSubject.value ?? {
                id: data.user!.id,
                nome: data.user!.email ?? '',
                email: data.user!.email ?? '',
                cnpj: '',
              }
          )
        );
      }),
      tap(() => this.logger.log('Login bem-sucedido')),
      catchError((err) => {
        this.logger.error('Erro ao fazer login', err);
        return throwError(() => err);
      })
    );
  }

  /**
   * Encerra a sessão e redireciona para a tela de login.
   */
  logout(): Observable<void> {
    return from(this.supabase.client.auth.signOut()).pipe(
      tap(() => {
        this.userSubject.next(null);
        this.router.navigate(['/login']);
      }),
      map(() => void 0)
    );
  }

  /**
   * Retorna o token de acesso atual.
   */
  getAccessToken(): string | null {
    return this.accessToken;
  }

  /**
   * Verifica se o usuário está autenticado.
   */
  isAuthenticated(): boolean {
    return !!this.userSubject.value;
  }

  /**
   * Retorna o usuário atual.
   */
  getCurrentUser(): Usuario | null {
    return this.userSubject.value;
  }
}
