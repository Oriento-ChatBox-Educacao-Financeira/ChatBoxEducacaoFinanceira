import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MainNavbar } from '../../_components/main-navbar/main-navbar';
import { ChatWidget } from "../../_components/chat-widget/chat-widget";

@Component({
  selector: 'app-dashboard-fluxo-caixa',
  standalone: true,
  imports: [CommonModule, MainNavbar, ChatWidget],
  templateUrl: './dashboard-fluxo-de-caixa.html',
  styleUrl: './dashboard-fluxo-de-caixa.css',
})
export class DashboardFluxoCaixa {
  kpis = [
    {
      label: 'SALDO ATUAL',
      value: 'R$ 2.450M',
      badge: '+12.5% vs mês ant.',
      badgeType: 'gold',
      icon: 'ph ph-wallet',
    },
    {
      label: 'ENTRADAS',
      value: 'R$ 845K',
      badge: '+8.3% no período',
      badgeType: 'blue',
      icon: 'ph ph-arrow-circle-down',
    },
    {
      label: 'SAÍDAS',
      value: 'R$ 402K',
      badge: '-4.1% no período',
      badgeType: 'neutral',
      icon: 'ph ph-arrow-circle-up',
    },
    {
      label: 'PROJEÇÃO 30 DIAS',
      value: 'R$ 2.89M',
      sub: 'Superávit previsto',
      highlight: true,
      icon: 'ph ph-chart-line-up',
    },
  ];

  vencimentosHoje = [
    {
      descricao: 'Folha de Pagamento',
      valor: 'R$ 185.000',
    },
    {
      descricao: 'Impostos (DAS)',
      valor: 'R$ 42.300',
    },
  ];

  movimentacoes = [
    {
      data: 'Hoje, 14:30',
      titulo: 'Recebimento NF-e 4592',
      subtitulo: 'Cliente: Alpha Solutions S.A.',
      categoria: 'Receitas',
      valor: 'R$ 45.000,00',
      entrada: true,
    },
    {
      data: 'Ontem, 10:15',
      titulo: 'Pagamento Fornecedor AWS',
      subtitulo: 'Infraestrutura em Nuvem',
      categoria: 'Custos Fixos',
      valor: 'R$ -12.450,00',
      entrada: false,
    },
    {
      data: '24 Nov, 09:00',
      titulo: 'Investimento CDB Liquidez Diária',
      subtitulo: 'Aplicação Automática',
      categoria: 'Aplicações',
      valor: 'R$ -150.000,00',
      entrada: false,
    },
  ];
}
