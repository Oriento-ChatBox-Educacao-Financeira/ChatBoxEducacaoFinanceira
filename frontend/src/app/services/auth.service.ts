import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';

interface LoginResponse {
  accessToken: string;
  expiresIn: number;
  usuario?: any;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private api = 'http://localhost:8080/api/auth'; // Backend API base
  private accessToken: string | null = null;
  private userSubject = new BehaviorSubject<any | null>(null);
  user$ = this.userSubject.asObservable();

  constructor(private http: HttpClient) {}

  register(payload: { nome: string; email: string; senha: string; cnpj: string }) {
    return this.http.post(`${this.api}/register`, payload);
  }

  // Realiza o login e salva o token no localStorage
  login(email: string, senha: string): Observable<any> {
    return this.http.post<LoginResponse>(`${this.api}/login`, { email, senha }).pipe(
      tap((res) => {
        this.accessToken = res.accessToken;
        this.userSubject.next(res.usuario);
      })
    );
  }

  //  Faz a renovação do token expirado
  refreshToken(): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.api}/refresh`, {}, { withCredentials: true }).pipe(
      tap((res) => (this.accessToken = res.accessToken)),
      catchError((err) => {
        this.logout();
        return throwError(() => err);
      })
    );
  }

  logout() {
    this.http.post(`${this.api}/logout`, {}, { withCredentials: true }).subscribe();
    this.accessToken = null;
    this.userSubject.next(null);
  }

  // Pega o token salvo (para enviar nas requisições)
  getAccessToken(): string | null {
    return this.accessToken;
  }

  // Verifica se o usuário está autenticado
  isAuthenticated(): boolean {
    return !!this.accessToken;
  }
}
