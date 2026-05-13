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

export class EmailAlreadyRegisteredError extends Error {
  constructor() {
    super('E-mail já cadastrado.');
    this.name = 'EmailAlreadyRegisteredError';
  }
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private userSubject = new BehaviorSubject<Usuario | null>(null);
  private accessToken: string | null = null;
  private authReadySubject = new BehaviorSubject<boolean>(false);

  user$: Observable<Usuario | null> = this.userSubject.asObservable();
  /**
   * Emite `true` quando a hidratação inicial da sessão termina (com sucesso ou
   * falha). Guards e telas que decidem rotas no boot devem aguardar isto antes
   * de chamar `isAuthenticated()`, pois a hidratação é assíncrona.
   */
  authReady$: Observable<boolean> = this.authReadySubject.asObservable();

  constructor(
    private supabase: SupabaseClientService,
    private router: Router,
    private logger: LoggerService
  ) {
    this.bootstrapSession().catch((err) => {
      this.logger.error('Erro inesperado em bootstrapSession', err);
      this.accessToken = null;
      this.userSubject.next(null);
      this.authReadySubject.next(true);
    });
    this.listenAuthChanges();
  }

  /**
   * Promise que resolve quando a hidratação inicial completa.
   * Útil para `AuthGuard` que precisa rodar de forma síncrona-aparente.
   */
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
   * Restaura a sessão persistida pelo supabase-js (localStorage) ao iniciar.
   * Se o perfil não puder ser carregado, derruba a sessão para evitar
   * estado inconsistente (token em memória sem usuário).
   */
  private async bootstrapSession(): Promise<void> {
    try {
      const { data, error } = await this.supabase.client.auth.getSession();
      if (error) {
        this.logger.warn('Falha ao recuperar sessão Supabase', error);
        return;
      }
      if (!data.session) return;

      this.accessToken = data.session.access_token;
      try {
        await this.hydrateUsuario(data.session.user.id);
      } catch (err) {
        this.logger.warn('Falha ao hidratar usuário ao iniciar; encerrando sessão', err);
        await this.supabase.client.auth.signOut();
        this.accessToken = null;
        this.userSubject.next(null);
      }
    } finally {
      this.authReadySubject.next(true);
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
      this.hydrateUsuario(session.user.id).catch(async (err) => {
        this.logger.warn('Falha ao hidratar usuário após auth change; encerrando sessão', err);
        await this.supabase.client.auth.signOut();
        this.accessToken = null;
        this.userSubject.next(null);
      });
    });
  }

  /**
   * Carrega o perfil em public.usuario (e empresa.cnpj) e emite no userSubject.
   * Lança em caso de erro ou perfil ausente — o chamador decide como reagir.
   */
  private async hydrateUsuario(authUserId: string): Promise<void> {
    const { data, error } = await this.supabase.client
      .from('usuario')
      .select('id_usuario,nome,email,empresa(cnpj,nome_fantasia,razao_social)')
      .eq('id_usuario', authUserId)
      .maybeSingle();

    if (error) {
      throw error;
    }
    if (!data) {
      throw new Error('Perfil de usuário não encontrado.');
    }

    const empresa = Array.isArray((data as any).empresa)
      ? (data as any).empresa[0]
      : (data as any).empresa;

    this.userSubject.next({
      id: data.id_usuario,
      nome: data.nome,
      email: data.email,
      cnpj: empresa?.cnpj ?? '',
      nomeFantasia: empresa?.nome_fantasia ?? undefined,
      razaoSocial: empresa?.razao_social ?? undefined,
    });
  }

  /**
   * Registra um novo usuário no Supabase Auth.
   * As linhas em public.usuario e public.empresa são criadas pela trigger
   * handle_new_user a partir do raw_user_meta_data.
   *
   * Detecta o caso silencioso do Supabase quando "Confirm email" está ligado:
   * para um e-mail já existente a API responde sem `error` e com `identities: []`.
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
        if (Array.isArray(data.user.identities) && data.user.identities.length === 0) {
          throw new EmailAlreadyRegisteredError();
        }
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
   * Falha explicitamente se o perfil não puder ser carregado.
   */
  login(email: string, senha: string): Observable<Usuario> {
    return from(
      this.supabase.client.auth.signInWithPassword({ email, password: senha })
    ).pipe(
      switchMap(({ data, error }) => {
        if (error) return throwError(() => error);
        if (!data.user || !data.session) {
          return throwError(() => new Error('Sessão inválida.'));
        }
        // Aplica o token imediatamente para evitar janela de corrida em que
        // requisições subsequentes sairiam sem Authorization até o callback
        // de onAuthStateChange rodar.
        this.accessToken = data.session.access_token;
        return from(this.hydrateUsuario(data.user.id)).pipe(
          switchMap(() => {
            const usuario = this.userSubject.value;
            if (!usuario) {
              return throwError(() => new Error('Perfil indisponível após login.'));
            }
            return [usuario];
          })
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
   * Baseia-se no token (sincronizado em bootstrapSession/login antes da
   * hidratação do perfil) para que o AuthGuard não derrube o usuário só
   * porque a busca do perfil ainda não terminou.
   */
  isAuthenticated(): boolean {
    return !!this.accessToken;
  }

  /**
   * Retorna o usuário atual.
   */
  getCurrentUser(): Usuario | null {
    return this.userSubject.value;
  }
}
