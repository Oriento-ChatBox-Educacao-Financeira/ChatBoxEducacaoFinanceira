import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';

const ROTAS_IMPLEMENTADAS = new Set([
  'dre',
  'fluxo-caixa',
  'balanco-patrimonial',
  'visao-geral',
  'chat',
]);

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class Navbar implements OnInit {
  isDashboardOpen = true;
  activeRoute = '';

  constructor(private router: Router) {}

  ngOnInit(): void {
    this.atualizarRotaAtiva(this.router.url);

    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((e) => {
        this.atualizarRotaAtiva(e.urlAfterRedirects);
      });
  }

  toggleDashboard(): void {
    this.isDashboardOpen = !this.isDashboardOpen;
  }

  navigate(route: string): void {
    // Atualiza item ativo
    this.activeRoute = route;

    // CHAT
    if (route === 'chat') {
      this.isDashboardOpen = false;

      this.router.navigate(['/chat']);
      return;
    }

    // DASHBOARD
    if (ROTAS_IMPLEMENTADAS.has(route)) {
      this.isDashboardOpen = true;

      this.router.navigate(['/dashboard', route]);
      return;
    }

    // OUTRAS ROTAS
    this.isDashboardOpen = false;
  }

  novoLancamento(): void {
    // TODO
  }

  sair(): void {
    this.router.navigate(['/login']);
  }

  private atualizarRotaAtiva(url: string): void {
    // CHAT
    if (url.startsWith('/chat')) {
      this.activeRoute = 'chat';
      this.isDashboardOpen = false;
      return;
    }

    // DASHBOARD
    const dashboardMatch = url.match(/^\/dashboard\/([^/?#]+)/);

    if (dashboardMatch) {
      this.activeRoute = dashboardMatch[1];
      this.isDashboardOpen = true;
      return;
    }

    // OUTRAS ROTAS
    this.activeRoute = '';
    this.isDashboardOpen = false;
  }
}
