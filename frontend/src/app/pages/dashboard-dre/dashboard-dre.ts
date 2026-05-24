import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MainNavbar } from "../../_components/main-navbar/main-navbar";
import { ChatWidget } from "../../_components/chat-widget/chat-widget";

@Component({
  selector: 'app-dashboard-dre',
  standalone: true,
  imports: [CommonModule, MainNavbar, ChatWidget],
  templateUrl: './dashboard-dre.html',
  styleUrl: './dashboard-dre.css',
})
export class DashboardDre {
  kpis = [
    {
      label: 'RECEITA BRUTA',
      value: 'R$ 2.45M',
      badge: '+12.5% vs ano ant.',
      badgeType: 'gold',
      icon: '/dashboard/icon-trending-up.svg',
    },
    {
      label: 'DESPESAS OPERACIONAIS',
      value: 'R$ 1.12M',
      badge: '-3.2% vs ano ant.',
      badgeType: 'neutral',
      icon: '/dashboard/icon-trending-down.svg',
    },
    {
      label: 'LUCRO LÍQUIDO',
      value: 'R$ 980K',
      sub: 'Consolidado',
      highlight: true,
      icon: '/dashboard/icon-trophy.svg',
    },
    {
      label: 'MARGEM DE LUCRO',
      value: '40%',
      progress: 40,
      icon: '/dashboard/icon-percent.svg',
    },
  ];

  dreItems = [
    { label: 'Receita Bruta',               valor: '2.450.000', bold: true  },
    { label: '(-) Deduções e Impostos',      valor: '(350.000)', sub: true   },
    { label: 'Receita Líquida',              valor: '2.100.000', bold: true, separator: true },
    { label: '(-) Custo das Mercadorias (CMV)', valor: '(450.000)', sub: true },
    { label: 'Lucro Bruto',                  valor: '1.650.000', blue: true, separator: true },
    { label: '(-) Despesas Operacionais',    valor: '(670.000)', sub: true   },
  ];
}
