import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MainNavbar } from '../../_components/main-navbar/main-navbar';
import { ChatWidget } from '../../_components/chat-widget/chat-widget';
import { ChatInfoButton } from '../../_components/chat-info-button/chat-info-button';
import { Skeleton } from '../../_components/skeleton/skeleton';
import { DashboardService } from '../../services/dashboard.service';

@Component({
  selector: 'app-dashboard-fluxo-caixa',
  standalone: true,
  imports: [CommonModule, RouterLink, MainNavbar, ChatWidget, ChatInfoButton, Skeleton],
  templateUrl: './dashboard-fluxo-de-caixa.html',
  styleUrl: './dashboard-fluxo-de-caixa.css',
})
export class DashboardFluxoCaixa implements OnInit {
  carregando = true;
  semDados = true;

  perguntasFluxo: string[] = [
    'Analise meu fluxo de caixa atual',
    'Quando devo me preocupar com saldo negativo?',
    'Sugira melhorias na gestão de caixa',
  ];

  kpis: {
    label: string;
    value: string;
    sub?: string;
    highlight?: boolean;
    icon?: string;
    semantic?: 'positive' | 'negative' | 'neutral';
  }[] = [];

  entradasPath = '';
  saidasPath = '';
  entradasPoints: { x: number; y: number; label: string; fullLabel: string }[] = [];
  saidasPoints: { x: number; y: number; label: string; fullLabel: string }[] = [];
  meses: string[] = [];
  fluxoMensal: { mes: string; entrada: number; saida: number; saldo: number }[] = [];

  totalEntradas = 0;
  totalSaidas = 0;
  totalSaldo = 0;
  melhorMes: { mes: string; saldo: number } | null = null;
  piorMes: { mes: string; saldo: number } | null = null;

  vencimentosHoje: { descricao: string; valor: string }[] = [];
  movimentacoes: {
    data: string;
    titulo: string;
    subtitulo: string;
    categoria: string;
    valor: string;
    entrada: boolean;
  }[] = [];

  constructor(private dashboardService: DashboardService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.dashboardService.fluxoCaixa().subscribe({
      next: (data) => {
        this.carregando = false;
        if (!data.hasData || !data.kpis) {
          this.semDados = true;
          this.cdr.markForCheck();
          return;
        }
        this.semDados = false;
        const k = data.kpis;
        this.kpis = [
          {
            label: 'SALDO DO PERÍODO',
            value: this.fmt(k.saldoAtual),
            icon: 'ph ph-wallet',
            highlight: true,
            semantic: k.saldoAtual >= 0 ? 'positive' : 'negative',
          },
          {
            label: 'ENTRADAS',
            value: this.fmt(k.entradas),
            icon: 'ph ph-arrow-down-right',
            semantic: 'positive',
          },
          {
            label: 'SAÍDAS',
            value: this.fmt(k.saidas),
            icon: 'ph ph-arrow-up-right',
            semantic: 'negative',
          },
          {
            label: 'RESULTADO MÉDIO',
            value: this.fmt(
              (data.evolucao?.length ?? 0) > 0
                ? k.saldoAtual / (data.evolucao?.length ?? 1)
                : 0,
            ),
            sub: 'média mensal',
            icon: 'ph ph-chart-line',
            semantic: 'neutral',
          },
        ];

        this.movimentacoes = (data.movimentacoes ?? []).map((m) => ({
          data: m.periodo,
          titulo: m.descricao,
          subtitulo: '',
          categoria: m.entrada ? 'Entrada' : 'Saída',
          valor: this.fmt(m.valor),
          entrada: m.entrada,
        }));

        const evol = data.evolucao ?? [];
        this.meses = evol.map((e) => e.mes);
        this.buildChart(
          evol.map((e) => e.entrada),
          evol.map((e) => e.saida),
        );
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

  private buildChart(entradas: number[], saidas: number[]): void {
    if (!entradas.length) {
      this.entradasPath = '';
      this.saidasPath = '';
      this.entradasPoints = [];
      this.saidasPoints = [];
      this.fluxoMensal = [];
      return;
    }
    const W = 580;
    const H = 220;
    const padX = 40;
    const padTop = 24;
    const padBottom = 28;
    const max = Math.max(...entradas, ...saidas, 1);
    const stepX = entradas.length === 1 ? 0 : (W - padX * 2) / (entradas.length - 1);
    const toPoint = (v: number, i: number) => ({
      x: padX + i * stepX,
      y: padTop + (1 - v / max) * (H - padTop - padBottom),
      label: this.fmtCompact(v),
      fullLabel: this.fmtFull(v),
    });
    this.entradasPoints = entradas.map(toPoint);
    this.saidasPoints = saidas.map(toPoint);
    this.entradasPath = this.entradasPoints.map((p) => `${p.x},${p.y}`).join(' ');
    this.saidasPath = this.saidasPoints.map((p) => `${p.x},${p.y}`).join(' ');
    this.fluxoMensal = entradas.map((e, i) => ({
      mes: this.meses[i] ?? `M${i + 1}`,
      entrada: e,
      saida: saidas[i],
      saldo: e - saidas[i],
    }));

    this.totalEntradas = entradas.reduce((a, v) => a + v, 0);
    this.totalSaidas = saidas.reduce((a, v) => a + v, 0);
    this.totalSaldo = this.totalEntradas - this.totalSaidas;

    if (this.fluxoMensal.length) {
      this.melhorMes = this.fluxoMensal.reduce((best, cur) =>
        cur.saldo > best.saldo ? cur : best,
      );
      this.piorMes = this.fluxoMensal.reduce((worst, cur) =>
        cur.saldo < worst.saldo ? cur : worst,
      );
    } else {
      this.melhorMes = null;
      this.piorMes = null;
    }
  }
}
