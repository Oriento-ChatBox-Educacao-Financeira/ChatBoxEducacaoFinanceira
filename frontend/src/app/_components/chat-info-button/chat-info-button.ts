import {
  Component,
  ElementRef,
  HostListener,
  Input,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

/**
 * Bot\u00e3o pequeno (\u00edcone "i") que abre um popover com 2-3 perguntas
 * pr\u00e9-definidas. Ao clicar uma pergunta, navega para /chat passando
 * a pergunta como query param para envio autom\u00e1tico.
 */
@Component({
  selector: 'app-chat-info-button',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './chat-info-button.html',
  styleUrl: './chat-info-button.css',
})
export class ChatInfoButton {
  @Input() perguntas: string[] = [];
  @Input() contextoLabel = '';
  @Input() titulo = 'Pergunte ao Oriento';

  open = signal(false);

  constructor(
    private host: ElementRef<HTMLElement>,
    private router: Router,
  ) {}

  toggle(ev?: MouseEvent): void {
    ev?.stopPropagation();
    this.open.update((v) => !v);
  }

  fechar(): void {
    this.open.set(false);
  }

  selecionar(pergunta: string): void {
    this.fechar();
    const queryParams: Record<string, string> = { pergunta };
    if (this.contextoLabel) {
      queryParams['contexto'] = this.contextoLabel;
    }
    this.router.navigate(['/chat'], { queryParams });
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(ev: MouseEvent): void {
    if (!this.open()) return;
    const target = ev.target as Node | null;
    if (target && !this.host.nativeElement.contains(target)) {
      this.fechar();
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open()) this.fechar();
  }
}
