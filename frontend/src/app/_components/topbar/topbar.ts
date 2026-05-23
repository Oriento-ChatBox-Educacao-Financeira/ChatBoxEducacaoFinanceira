import { Component, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './topbar.html',
  styleUrl: './topbar.css',
})
export class Topbar {
  @Input() userName = 'Alexander Fiscal';

  searchQuery = '';

  constructor(private router: Router) {}

  abrirNotificacoes(): void {
    this.router.navigate(['/dashboard/notificacoes']);
  }
}
