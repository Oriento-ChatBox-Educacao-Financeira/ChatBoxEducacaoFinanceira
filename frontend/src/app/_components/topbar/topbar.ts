import { Component, Input, OnInit, HostListener, ElementRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NotificacaoService, Notificacao } from '../../services/notificacao.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './topbar.html',
  styleUrl: './topbar.css',
})
export class Topbar implements OnInit {
  @Input() userName = 'Alexander Fiscal';

  searchQuery = '';
  dropdownAberto = false;
  perfilAberto = false;

  constructor(
    private router: Router,
    public notif: NotificacaoService,
    private host: ElementRef,
    private auth: AuthService,
  ) {}

  async ngOnInit(): Promise<void> {
    await this.notif.carregar();
  }

  @HostListener('document:click', ['$event'])
  onDocClick(ev: MouseEvent): void {
    const inside = (this.host.nativeElement as HTMLElement).contains(ev.target as Node);
    if (!inside) {
      this.dropdownAberto = false;
      this.perfilAberto = false;
    }
  }

  togglePerfil(): void {
    this.perfilAberto = !this.perfilAberto;
    if (this.perfilAberto) this.dropdownAberto = false;
  }

  irPara(rota: string): void {
    this.perfilAberto = false;
    this.router.navigate([rota]);
  }

  sair(): void {
    this.perfilAberto = false;
    this.auth.logout().subscribe();
  }

  async toggleNotificacoes(): Promise<void> {
    this.dropdownAberto = !this.dropdownAberto;
    if (this.dropdownAberto) {
      this.perfilAberto = false;
      await this.notif.carregar();
    }
  }

  async clicarNotificacao(n: Notificacao): Promise<void> {
    if (!n.lida) await this.notif.marcarComoLida(n.id_notificacao);
    if (n.link) {
      this.dropdownAberto = false;
      this.router.navigate([n.link]);
    }
  }

  async marcarTodasLidas(): Promise<void> {
    await this.notif.marcarTodasLidas();
  }

  formatRelativo(iso: string): string {
    const diff = (Date.now() - new Date(iso).getTime()) / 1000;
    if (diff < 60) return 'agora';
    if (diff < 3600) return `${Math.floor(diff / 60)}min`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h`;
    if (diff < 7 * 86400) return `${Math.floor(diff / 86400)}d`;
    return new Date(iso).toLocaleDateString('pt-BR');
  }
}
