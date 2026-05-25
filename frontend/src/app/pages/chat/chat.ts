import { Component, signal, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { MainNavbar } from '../../_components/main-navbar/main-navbar';

interface Conversation {
  title: string;
  date: string;
}

interface InsightCard {
  label: string;
  value: string;
  description: string;
}

interface StrategyItem {
  text: string;
}

interface BotCard {
  icon: string;
  title: string;
  text: string;
  insights?: InsightCard[];
  strategy?: { title: string; items: StrategyItem[] };
}

interface ChatMessage {
  role: 'user' | 'bot';
  text?: string;
  card?: BotCard;
  time: string;
}

@Component({
  selector: 'app-chat-page',
  standalone: true,
  imports: [CommonModule, FormsModule, MainNavbar],
  templateUrl: './chat.html',
  styleUrl: './chat.css',
})
export class ChatPageComponent implements AfterViewChecked {
  @ViewChild('scrollEl') scrollEl?: ElementRef<HTMLElement>;
  @ViewChild('textarea') textarea?: ElementRef<HTMLTextAreaElement>;

  message = '';
  drawerOpen = signal(false);
  shouldScroll = false;

  suggestions: string[] = [
    'Explicar minha DRE',
    'Projetar fluxo de caixa',
    'Analisar margem operacional',
    'Reduzir despesas',
  ];

  conversations: Conversation[] = [
    { title: 'Análise de investimentos', date: 'Hoje' },
    { title: 'Fluxo de caixa', date: 'Ontem' },
    { title: 'Indicadores financeiros', date: '22 Mai' },
  ];

  messages = signal<ChatMessage[]>([
    {
      role: 'user',
      text:
        'Estou procurando diversificar minha carteira. Você pode analisar as tendências atuais do mercado de ações de energia sustentável e sugerir uma estratégia de entrada de baixo risco?',
      time: '10:24',
    },
    {
      role: 'bot',
      time: '10:24',
      card: {
        icon: 'ph-chart-line-up',
        title: 'Análise de Energia Sustentável',
        text:
          'O setor de energia sustentável está passando por uma mudança significativa em direção à <strong>infraestrutura distribuída</strong>. Com base em dados fiscais recentes, observamos um aumento de 14% na alocação de capital de longo prazo no mercado europeu, especificamente para soluções de armazenamento em escala de rede.',
        insights: [
          { label: 'PREVISÃO DE CRESCIMENTO', value: '+22.4%', description: 'CAGR estimado do setor até 2027' },
          { label: 'AVALIAÇÃO DE RISCO', value: 'Moderado', description: 'Sensibilidade alta às políticas fiscais' },
        ],
        strategy: {
          title: 'Estratégia Recomendada',
          items: [
            { text: 'Utilize <strong>DCA (Dollar Cost Averaging)</strong> em uma janela de 6 meses para mitigar volatilidade.' },
            { text: 'Foque em empresas de infraestrutura energética em vez de manufatura pesada.' },
            { text: 'Mantenha uma reserva de caixa de 15% para entradas oportunistas.' },
          ],
        },
      },
    },
  ]);

  ngAfterViewChecked(): void {
    if (this.shouldScroll && this.scrollEl) {
      this.scrollEl.nativeElement.scrollTop = this.scrollEl.nativeElement.scrollHeight;
      this.shouldScroll = false;
    }
  }

  sendSuggestion(text: string): void {
    this.message = text;
    this.textarea?.nativeElement.focus();
    this.autoResize();
  }

  enviar(): void {
    const txt = this.message.trim();
    if (!txt) return;
    const now = new Date();
    const time = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
    this.messages.update((arr) => [...arr, { role: 'user', text: txt, time }]);
    this.message = '';
    this.shouldScroll = true;
    setTimeout(() => this.autoResize());
  }

  onKeydown(ev: KeyboardEvent): void {
    if (ev.key === 'Enter' && !ev.shiftKey) {
      ev.preventDefault();
      this.enviar();
    }
  }

  autoResize(): void {
    const el = this.textarea?.nativeElement;
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 200) + 'px';
  }

  novaConversa(): void {
    this.messages.set([]);
    this.message = '';
  }

  toggleDrawer(): void {
    this.drawerOpen.update((v) => !v);
  }

  fecharDrawer(): void {
    this.drawerOpen.set(false);
  }
}
