import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MainNavbar } from '../../_components/main-navbar/main-navbar';
import { ChatWidget } from '../../_components/chat-widget/chat-widget';
import { DashboardService, DreItem } from '../../services/dashboard.service';

interface ChartPoint {
  x: number;
  y: number;
  label: string;
  fullLabel: string;
}

interface KpiCard {
  label: string;
  value: string;
  badge?: string;
  badgeType?: string;
  sub?: string;
  highlight?: boolean;
  progress?: number;
  icon?: string;
}

@Component({
  selector: 'app-dashboard-dre',
  standalone: true,
  imports: [CommonModule, RouterLink, MainNavbar, ChatWidget],
  templateUrl: './dashboard-dre.html',
  styleUrl: './dashboard-dre.css',
})
export class DashboardDre implements OnInit {
  carregando = true;
  semDados = true;

  kpis: KpiCard[] = [];
  dreItems: DreItem[] = [];

  // Gráfico Evolução de Resultado — pontos calculados a partir da DRE
  receitaPath = '';
  despesaPath = '';
  receitaPoints: ChartPoint[] = [];
  despesaPoints: ChartPoint[] = [];
  meses: string[] = [];
  resultadoMensal: { mes: string; receita: number; despesa: number; resultado: number }[] = [];

  constructor(private dashboardService: DashboardService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.dashboardService.dre().subscribe({
      next: (data) => {
        this.carregando = false;
        if (!data.hasData || !data.kpis) {
          this.semDados = true;
          this.cdr.markForCheck();
          return;
        }
        this.semDados = false;
        this.kpis = [
          {
            label: 'RECEITA BRUTA',
            value: this.formatMoeda(data.kpis.receitaBruta),
            badge: 'período consolidado',
            badgeType: 'gold',
          },
          {
            label: 'DESPESAS OPERACIONAIS',
            value: this.formatMoeda(data.kpis.despOperacionais),
            badge: 'período consolidado',
            badgeType: 'neutral',
          },
          {
            label: 'LUCRO LÍQUIDO',
            value: this.formatMoeda(data.kpis.lucroLiquido),
            sub: 'Consolidado',
            highlight: true,
          },
          {
            label: 'MARGEM DE LUCRO',
            value: `${data.kpis.margemLucro.toFixed(1)}%`,
            progress: Math.max(0, Math.min(100, data.kpis.margemLucro)),
          },
        ];
        this.dreItems = data.itens ?? [];

        const evol = data.evolucao ?? [];
        this.meses = evol.map((e) => e.mes);
        const receita = evol.map((e) => e.receita);
        const despesa = evol.map((e) => e.despesa);
        this.buildResultChart(receita, despesa);
        this.cdr.markForCheck();
      },
      error: () => {
        this.carregando = false;
        this.semDados = true;
        this.cdr.markForCheck();
      },
    });
  }

  formatMoeda(v: number): string {
    if (Math.abs(v) >= 1_000_000) return `R$ ${(v / 1_000_000).toFixed(2)}M`;
    if (Math.abs(v) >= 1_000) return `R$ ${(v / 1_000).toFixed(0)}K`;
    return `R$ ${v.toFixed(2)}`;
  }

  formatValor(v: number): string {
    const abs = Math.abs(v).toLocaleString('pt-BR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
    return v < 0 ? `(${abs})` : abs;
  }

  fmtCompact(v: number): string {
    const abs = Math.abs(v);
    if (abs >= 1_000_000) return `R$ ${(v / 1_000_000).toFixed(1)}M`;
    if (abs >= 1_000) return `R$ ${(v / 1_000).toFixed(0)}K`;
    return `R$ ${v.toFixed(0)}`;
  }

  fmtFull(v: number): string {
    return v.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL',
      maximumFractionDigits: 0,
    });
  }

  private buildResultChart(receita: number[], despesa: number[]): void {
    if (!receita.length) {
      this.receitaPath = '';
      this.despesaPath = '';
      this.receitaPoints = [];
      this.despesaPoints = [];
      this.resultadoMensal = [];
      return;
    }
    const W = 580;
    const H = 220;
    const padX = 40;
    const padTop = 24;
    const padBottom = 28;
    const all = [...receita, ...despesa];
    const max = Math.max(...all, 1);
    const stepX = receita.length === 1 ? 0 : (W - padX * 2) / (receita.length - 1);
    const toPoint = (v: number, i: number): ChartPoint => {
      const x = padX + i * stepX;
      const y = padTop + (1 - v / max) * (H - padTop - padBottom);
      return { x, y, label: this.fmtCompact(v), fullLabel: this.fmtFull(v) };
    };
    this.receitaPoints = receita.map(toPoint);
    this.despesaPoints = despesa.map(toPoint);
    this.receitaPath = this.receitaPoints.map((p) => `${p.x},${p.y}`).join(' ');
    this.despesaPath = this.despesaPoints.map((p) => `${p.x},${p.y}`).join(' ');
    this.resultadoMensal = receita.map((r, i) => ({
      mes: this.meses[i] ?? `M${i + 1}`,
      receita: r,
      despesa: despesa[i],
      resultado: r - despesa[i],
    }));
  }
}
