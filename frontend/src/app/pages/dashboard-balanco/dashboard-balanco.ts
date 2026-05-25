import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MainNavbar } from '../../_components/main-navbar/main-navbar';
import { ChatWidget } from '../../_components/chat-widget/chat-widget';
import { DashboardService } from '../../services/dashboard.service';

@Component({
  selector: 'app-dashboard-balanco-patrimonial',
  standalone: true,
  imports: [CommonModule, RouterLink, MainNavbar, ChatWidget],
  templateUrl: './dashboard-balanco.html',
  styleUrl: './dashboard-balanco.css',
})
export class DashboardBalancoPatrimonial implements OnInit {
  carregando = true;
  semDados = true;

  periodoAtivo = '';
  periodos: string[] = [];

  kpis: { label: string; value: string; sub?: string; highlight?: boolean; badge?: string; badgeType?: string; icon?: string }[] = [];
  ativos: { nome: string; desc: string; percentual: string; valor: string; color: string }[] = [];
  recursos: { nome: string; desc: string; percentual: string; valor: string; color: string }[] = [];

  indicadores: { label: string; value: string }[] = [];

  patrimonioPath = '';
  patrimonioPoints: { x: number; y: number; label: string; fullLabel: string }[] = [];
  rotulosPeriodos: string[] = [];
  evolucao: { periodo: string; valor: number }[] = [];
  patrimonioLiquidoTotal = 0;
  liquidezAtual: number | null = null;

  constructor(private dashboardService: DashboardService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.dashboardService.balanco().subscribe({
      next: (data) => {
        this.carregando = false;
        if (!data.hasData || !data.kpis) {
          this.semDados = true;
          this.cdr.markForCheck();
          return;
        }
        this.semDados = false;
        const k = data.kpis;
        this.patrimonioLiquidoTotal = k.patrimonioLiquido;
        this.liquidezAtual = k.liquidez;

        this.kpis = [
          { label: 'TOTAL DE ATIVOS', value: this.fmt(k.totalAtivo) },
          { label: 'TOTAL DE PASSIVOS', value: this.fmt(k.totalPassivo) },
          {
            label: 'ÍNDICE DE LIQUIDEZ',
            value: k.liquidez != null ? k.liquidez.toFixed(2) : '—',
            sub: 'Capacidade de pagamento',
          },
          {
            label: 'PATRIMÔNIO LÍQUIDO',
            value: this.fmt(k.patrimonioLiquido),
            highlight: true,
          },
        ];

        const totalAtivo = k.totalAtivo || 1;
        const totalRecursos = k.totalPassivo + k.patrimonioLiquido || 1;

        const corA = ['#103688', '#002162', '#3C5AAD'];
        this.ativos = (data.ativos ?? []).map((a, i) => ({
          nome: a.nome,
          desc: a.desc,
          percentual: `${((a.valor / totalAtivo) * 100).toFixed(0)}%`,
          valor: this.fmt(a.valor),
          color: corA[i] ?? '#103688',
        }));

        const corP = ['#FEB700', '#7C5800', '#191C1D'];
        this.recursos = (data.recursos ?? []).map((r, i) => ({
          nome: r.nome,
          desc: r.desc,
          percentual: `${((r.valor / totalRecursos) * 100).toFixed(0)}%`,
          valor: this.fmt(r.valor),
          color: corP[i] ?? '#191C1D',
        }));

        const evol = data.evolucao ?? [];
        this.rotulosPeriodos = evol.map((e) => e.rotulo);
        this.periodos = evol.map((e) => e.rotulo).slice(-3).reverse();
        this.periodoAtivo = this.periodos[0] ?? '';
        this.buildPatrimonioChart(evol.map((e) => e.patrimonio));

        this.indicadores = [
          {
            label: 'Capital de Giro',
            value: this.fmt((data.ativos?.[0]?.valor ?? 0) - (data.recursos?.[0]?.valor ?? 0)),
          },
          {
            label: 'Endividamento',
            value: k.totalAtivo
              ? `${((k.totalPassivo / k.totalAtivo) * 100).toFixed(1)}%`
              : '—',
          },
          {
            label: 'Liquidez Corrente',
            value: k.liquidez != null ? k.liquidez.toFixed(2) : '—',
          },
          {
            label: 'ROE',
            value: k.patrimonioLiquido
              ? `${(((data.kpis?.totalAtivo ?? 0) / k.patrimonioLiquido) * 100).toFixed(1)}%`
              : '—',
          },
        ];
        this.cdr.markForCheck();
      },
      error: () => {
        this.carregando = false;
        this.semDados = true;
        this.cdr.markForCheck();
      },
    });
  }

  private fmt(v: number): string {
    if (Math.abs(v) >= 1_000_000) return `R$ ${(v / 1_000_000).toFixed(2)}M`;
    if (Math.abs(v) >= 1_000) return `R$ ${(v / 1_000).toFixed(0)}K`;
    return `R$ ${v.toFixed(2)}`;
  }

  fmtCompact(v: number): string {
    const abs = Math.abs(v);
    if (abs >= 1_000_000) return `${(v / 1_000_000).toFixed(1)}M`;
    if (abs >= 1_000) return `${(v / 1_000).toFixed(0)}K`;
    return `${v.toFixed(0)}`;
  }

  fmtFull(v: number): string {
    return v.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL',
      maximumFractionDigits: 0,
    });
  }

  private buildPatrimonioChart(valores: number[]): void {
    if (!valores.length) {
      this.patrimonioPoints = [];
      this.patrimonioPath = '';
      this.evolucao = [];
      return;
    }
    const W = 580;
    const H = 220;
    const padX = 40;
    const padTop = 24;
    const padBottom = 28;
    const max = Math.max(...valores, 1);
    const stepX = valores.length === 1 ? 0 : (W - padX * 2) / (valores.length - 1);
    this.patrimonioPoints = valores.map((v, i) => ({
      x: padX + i * stepX,
      y: padTop + (1 - v / max) * (H - padTop - padBottom),
      label: this.fmtCompact(v),
      fullLabel: this.fmtFull(v),
    }));
    this.patrimonioPath = this.patrimonioPoints.map((p) => `${p.x},${p.y}`).join(' ');
    this.evolucao = valores.map((v, i) => ({
      periodo: this.rotulosPeriodos[i] ?? `P${i + 1}`,
      valor: v,
    }));
  }
}
