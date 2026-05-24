import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MainNavbar } from '../../_components/main-navbar/main-navbar';
import { ChatWidget } from "../../_components/chat-widget/chat-widget";

@Component({
  selector: 'app-dashboard-balanco-patrimonial',
  standalone: true,
  imports: [CommonModule, MainNavbar, ChatWidget],
  templateUrl: './dashboard-balanco.html',
  styleUrl: './dashboard-balanco.css',
})
export class DashboardBalancoPatrimonial {
  periodoAtivo = 'T3 2023';

  periodos = ['T3 2026', 'T2 2026', 'T1 2026', 'T4 2025', 'T3 2025', 'T2 2025'];

  kpis = [
    {
      label: 'TOTAL DE ATIVOS',
      value: 'R$ 14.285M',
      badge: '+4.2% vs trimestre',
      badgeType: 'green',
      icon: 'ph ph-bank',
    },
    {
      label: 'TOTAL DE PASSIVOS',
      value: 'R$ 6.842M',
      badge: '-0.5% vs trimestre',
      badgeType: 'neutral',
      icon: 'ph ph-credit-card',
    },
    {
      label: 'ÍNDICE DE LIQUIDEZ',
      value: '1.8',
      sub: 'Capacidade de pagamento saudável',
      icon: 'ph ph-chart-line-up',
    },
    {
      label: 'PATRIMÔNIO LÍQUIDO',
      value: 'R$ 7.443M',
      badge: '+8.1% crescimento',
      badgeType: 'green',
      highlight: true,
      icon: 'ph ph-chart-pie-slice',
    },
  ];

  indicadores = [
    {
      label: 'Capital de Giro',
      value: 'R$ 1.92M',
    },
    {
      label: 'Endividamento',
      value: '47.8%',
    },
    {
      label: 'Liquidez Corrente',
      value: '1.82',
    },
    {
      label: 'ROE',
      value: '14.2%',
    },
  ];

  ativos = [
    {
      nome: 'Circulante',
      desc: 'Disponibilidades e CP',
      percentual: '45%',
      valor: 'R$ 6.428k',
      color: '#103688',
    },
    {
      nome: 'Realizável LP',
      desc: 'Aplicações e Direitos',
      percentual: '30%',
      valor: 'R$ 4.285k',
      color: '#002162',
    },
    {
      nome: 'Permanente',
      desc: 'Imobilizado e Intangível',
      percentual: '25%',
      valor: 'R$ 3.572k',
      color: '#3C5AAD',
    },
  ];

  recursos = [
    {
      nome: 'Passivo Circulante',
      desc: 'Obrigações CP',
      percentual: '35%',
      valor: 'R$ 2.394k',
      color: '#FEB700',
    },
    {
      nome: 'Exigível LP',
      desc: 'Dívidas Longo Prazo',
      percentual: '20%',
      valor: 'R$ 1.368k',
      color: '#7C5800',
    },
    {
      nome: 'Patrimônio Líquido',
      desc: 'Capital Próprio',
      percentual: '45%',
      valor: 'R$ 3.078k',
      color: '#191C1D',
    },
  ];
}
