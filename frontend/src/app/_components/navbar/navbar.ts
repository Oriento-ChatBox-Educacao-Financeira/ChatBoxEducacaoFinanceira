import { CommonModule } from '@angular/common';
import { Component, OnInit, computed } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { UiStateService } from '../../services/ui-state.service';
import { NotificacaoService } from '../../services/notificacao.service';

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

  readonly collapsed = computed(() => this.ui.sidebarCollapsed());

  constructor(
    private router: Router,
    private ui: UiStateService,
    public notif: NotificacaoService,
  ) {}

  ngOnInit(): void {
    this.atualizarRotaAtiva(this.router.url);

    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((e) => {
        this.atualizarRotaAtiva(e.urlAfterRedirects);
      });
  }

  toggleSidebar(): void {
    this.ui.toggleSidebar();
  }

  toggleDashboard(): void {
    if (this.collapsed()) {
      this.ui.toggleSidebar();
      this.isDashboardOpen = true;
      return;
    }
    this.isDashboardOpen = !this.isDashboardOpen;
  }

  navigate(route: string): void {
    this.activeRoute = route;

    if (route === 'chat') {
      this.isDashboardOpen = false;
      this.router.navigate(['/chat']);
      return;
    }

    if (ROTAS_IMPLEMENTADAS.has(route)) {
      this.isDashboardOpen = true;
      this.router.navigate(['/dashboard', route]);
      return;
    }

    this.isDashboardOpen = false;
  }

  novoLancamento(): void {
    this.router.navigate(['/upload-planilha']);
  }

  sair(): void {
    this.router.navigate(['/login']);
  }

  private atualizarRotaAtiva(url: string): void {
    if (url.startsWith('/chat')) {
      this.activeRoute = 'chat';
      this.isDashboardOpen = false;
      return;
    }

    const dashboardMatch = url.match(/^\/dashboard\/([^/?#]+)/);

    if (dashboardMatch) {
      this.activeRoute = dashboardMatch[1];
      this.isDashboardOpen = true;
      return;
    }

    this.activeRoute = '';
    this.isDashboardOpen = false;
  }
}
