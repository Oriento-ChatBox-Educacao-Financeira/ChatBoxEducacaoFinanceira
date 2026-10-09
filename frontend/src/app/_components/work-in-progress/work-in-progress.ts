import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-work-in-progress',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './work-in-progress.html',
  styleUrl: './work-in-progress.css',
})
export class WorkInProgress {
  @Input() titulo = 'Em construção';
  @Input() descricao =
    'Esta seção está sendo preparada e ficará disponível em breve.';
  @Input() icone = 'ph ph-traffic-cone';
  @Input() rotaVoltar = '/dashboard/visao-geral';
  @Input() rotuloVoltar = 'Voltar para Visão Geral';

  constructor(private router: Router) {}

  voltar(): void {
    this.router.navigate([this.rotaVoltar]);
  }
}
