import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';

// Rotas já implementadas dentro de /dashboard. Conforme as próximas telas
// forem entrando, é só adicionar aqui que a sidebar passa a navegar.
const ROTAS_IMPLEMENTADAS = new Set(['dre', 'fluxo-caixa']);

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
      .subscribe((e) => this.atualizarRotaAtiva(e.urlAfterRedirects));
  }

  toggleDashboard(): void {
    this.isDashboardOpen = !this.isDashboardOpen;
  }

  navigate(route: string): void {
    this.activeRoute = route;
    if (ROTAS_IMPLEMENTADAS.has(route)) {
      this.router.navigate(['/dashboard', route]);
    }
  }

  novoLancamento(): void {
    // TODO: abrir modal ou navegar quando a tela existir
  }

  sair(): void {
    this.router.navigate(['/login']);
  }

  private atualizarRotaAtiva(url: string): void {
    const match = url.match(/^\/dashboard\/([^/?#]+)/);
    this.activeRoute = match ? match[1] : '';
  }
}
