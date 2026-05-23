import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class Navbar {
  isDashboardOpen = true;
  activeRoute = 'fluxo-caixa';

  constructor(private router: Router) {}

  toggleDashboard(): void {
    this.isDashboardOpen = !this.isDashboardOpen;
  }

  navigate(route: string): void {
    this.activeRoute = route;
    this.router.navigate(['/dashboard', route]);
  }

  novoLancamento(): void {
    // Abre modal ou navega para novo lançamento
    this.router.navigate(['/dashboard/novo-lancamento']);
  }

  sair(): void {
    this.router.navigate(['/login']);
  }
}
