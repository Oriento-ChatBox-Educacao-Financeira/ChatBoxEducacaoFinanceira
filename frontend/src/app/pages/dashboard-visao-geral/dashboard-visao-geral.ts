import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MainNavbar } from '../../_components/main-navbar/main-navbar';
import { ChatWidget } from '../../_components/chat-widget/chat-widget';
import { ChatInfoButton } from '../../_components/chat-info-button/chat-info-button';
import { Skeleton } from '../../_components/skeleton/skeleton';
import { DashboardService } from '../../services/dashboard.service';

@Component({
  selector: 'app-dashboard-visao-geral',
  standalone: true,
  imports: [CommonModule, RouterLink, MainNavbar, ChatWidget, ChatInfoButton, Skeleton],
  templateUrl: './dashboard-visao-geral.html',
  styleUrl: './dashboard-visao-geral.css',
})
export class DashboardVisaoGeral implements OnInit {
  carregando = true;
  semDados = true;

  perguntasVisao: string[] = [
    'Resumo da minha saúde financeira',
    'Quais são meus principais pontos fracos?',
    'Sugira próximos passos para crescer',
  ];

  periodoAtual = '';
  kpis: { label: string; value: string; sub?: string; badge?: string; icon: string }[] = [];
  dre: {
    mes: string;
    receita: string;
    despesa: string;
    receitaValor: number;
    despesaValor: number;
    receitaLabel: string;
    despesaLabel: string;
  }[] = [];
  vencimentos: { nome: string; desc: string; valor: string; icon: string }[] = [];

  constructor(private dashboardService: DashboardService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.dashboardService.visaoGeral().subscribe({
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
            label: 'Saldo Atual',
            value: this.fmt(k.saldoAtual),
            icon: 'ph ph-wallet',
          },
          {
            label: 'Lucro Líquido',
            value: this.fmt(k.lucroLiquido),
            sub: `Margem ${k.margem.toFixed(1)}%`,
            icon: 'ph ph-chart-line-up',
          },
          {
            label: 'Patrimônio Líquido',
            value: this.fmt(k.patrimonioLiquido),
            sub: 'Balanço Patrimonial',
            icon: 'ph ph-bank',
          },
          {
            label: 'Índice de Liquidez',
            value: k.liquidez != null ? k.liquidez.toFixed(2) : '—',
            sub: k.liquidez >= 1 ? 'Saudável (> 1.0)' : 'Atenção',
            icon: 'ph ph-drop',
          },
        ];

        const evolucao = data.evolucao ?? [];
        const maxAbs = Math.max(
          ...evolucao.flatMap((e) => [Math.abs(e.receita), Math.abs(e.despesa)]),
          1,
        );
        if (evolucao.length) this.periodoAtual = evolucao[evolucao.length - 1].mes;

        this.dre = evolucao.map((e) => ({
          mes: e.mes,
          receita: `${Math.max(8, (Math.abs(e.receita) / maxAbs) * 200)}px`,
          despesa: `${Math.max(8, (Math.abs(e.despesa) / maxAbs) * 200)}px`,
          receitaValor: e.receita,
          despesaValor: e.despesa,
          receitaLabel: this.fmtCompact(e.receita),
          despesaLabel: this.fmtCompact(e.despesa),
        }));
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
}
