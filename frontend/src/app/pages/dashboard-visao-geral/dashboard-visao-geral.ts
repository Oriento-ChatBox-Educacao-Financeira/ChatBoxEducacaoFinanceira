import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MainNavbar } from '../../_components/main-navbar/main-navbar';

@Component({
  selector: 'app-dashboard-visao-geral',
  standalone: true,
  imports: [CommonModule, MainNavbar],
  templateUrl: './dashboard-visao-geral.html',
  styleUrl: './dashboard-visao-geral.css',
})
export class DashboardVisaoGeral {
  kpis = [
    {
      label: 'Saldo Atual',
      value: 'R$ 2.450.000',
      badge: '+12.5% vs mês anterior',
      icon: 'ph ph-wallet',
    },
    {
      label: 'Lucro Líquido',
      value: 'R$ 1.33M',
      sub: 'Margem 54%',
      icon: 'ph ph-chart-line-up',
    },
    {
      label: 'Patrimônio Líquido',
      value: 'R$ 7.443.350',
      sub: 'Balanço Patrimonial',
      icon: 'ph ph-bank',
    },
    {
      label: 'Índice de Liquidez',
      value: '1.8',
      sub: 'Saudável (> 1.0)',
      icon: 'ph ph-drop',
    },
  ];

  dre = [
    {
      mes: 'Jul',
      receita: '120px',
      despesa: '80px',
    },
    {
      mes: 'Ago',
      receita: '150px',
      despesa: '90px',
    },
    {
      mes: 'Set',
      receita: '170px',
      despesa: '100px',
    },
    {
      mes: 'Out',
      receita: '140px',
      despesa: '70px',
    },
    {
      mes: 'Nov',
      receita: '190px',
      despesa: '80px',
    },
  ];

  vencimentos = [
    {
      nome: 'Folha de Pagamento',
      desc: 'Vence em 2 dias',
      valor: 'R$ 145K',
      icon: 'ph ph-users-three',
    },
    {
      nome: 'Impostos (DAS/DARF)',
      desc: 'Vence em 5 dias',
      valor: 'R$ 82K',
      icon: 'ph ph-file-text',
    },
  ];
}
