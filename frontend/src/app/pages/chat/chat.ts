import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { MainNavbar } from '../../_components/main-navbar/main-navbar';

@Component({
  selector: 'app-chat-page',
  standalone: true,
  imports: [CommonModule, FormsModule, MainNavbar],
  templateUrl: './chat.html',
  styleUrl: './chat.css',
})
export class ChatPageComponent {
  message = '';

  suggestions: string[] = [
    'Explicar minha DRE',
    'Projetar fluxo de caixa',
    'Analisar margem operacional',
    'Reduzir despesas',
  ];

  conversations = [
    {
      title: 'Análise de investimentos',
      date: 'Hoje',
    },
    {
      title: 'Fluxo de caixa',
      date: 'Ontem',
    },
    {
      title: 'Indicadores financeiros',
      date: '22 Mai',
    },
  ];

  sendSuggestion(text: string): void {
    this.message = text;
  }
}
