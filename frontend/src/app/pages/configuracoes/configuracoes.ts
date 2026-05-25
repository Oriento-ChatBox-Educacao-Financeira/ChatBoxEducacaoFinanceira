import { Component, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainNavbar } from '../../_components/main-navbar/main-navbar';
import { SupabaseClientService } from '../../services/supabase.client';
import { PlanilhaService, PlanilhaImportada } from '../../services/planilha.service';
import { LoggerService } from '../../services/logger.service';

@Component({
  selector: 'app-configuracoes',
  standalone: true,
  imports: [CommonModule, FormsModule, MainNavbar, DatePipe],
  templateUrl: './configuracoes.html',
  styleUrl: './configuracoes.css',
})
export class ConfiguracoesPage implements OnInit {
  // Senha
  novaSenha = '';
  confirmaSenha = '';
  senhaMsg = signal<{ tipo: 'sucesso' | 'erro'; texto: string } | null>(null);
  trocandoSenha = signal(false);

  // Planilhas
  planilhas = signal<PlanilhaImportada[]>([]);
  carregandoPlanilhas = signal(true);
  deletandoArquivo = signal<string | null>(null);

  constructor(
    private supabase: SupabaseClientService,
    private planilhaService: PlanilhaService,
    private logger: LoggerService,
  ) {}

  async ngOnInit(): Promise<void> {
    await this.recarregarPlanilhas();
  }

  async recarregarPlanilhas(): Promise<void> {
    this.carregandoPlanilhas.set(true);
    this.planilhas.set(await this.planilhaService.listar());
    this.carregandoPlanilhas.set(false);
  }

  async trocarSenha(): Promise<void> {
    this.senhaMsg.set(null);
    if (this.novaSenha.length < 6) {
      this.senhaMsg.set({ tipo: 'erro', texto: 'A senha precisa ter no mínimo 6 caracteres.' });
      return;
    }
    if (this.novaSenha !== this.confirmaSenha) {
      this.senhaMsg.set({ tipo: 'erro', texto: 'As senhas não coincidem.' });
      return;
    }
    this.trocandoSenha.set(true);
    const { error } = await this.supabase.client.auth.updateUser({ password: this.novaSenha });
    this.trocandoSenha.set(false);
    if (error) {
      this.logger.error('Falha ao trocar senha', error);
      this.senhaMsg.set({ tipo: 'erro', texto: error.message });
      return;
    }
    this.novaSenha = '';
    this.confirmaSenha = '';
    this.senhaMsg.set({ tipo: 'sucesso', texto: 'Senha atualizada com sucesso.' });
  }

  async deletar(p: PlanilhaImportada): Promise<void> {
    const ok = confirm(
      `Apagar a planilha "${p.arquivo}"?\n\nIsso vai remover ${p.linhas} linhas dos dashboards.`,
    );
    if (!ok) return;
    this.deletandoArquivo.set(p.arquivo);
    try {
      await this.planilhaService.deletar(p.arquivo);
      await this.recarregarPlanilhas();
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Falha ao apagar.');
    } finally {
      this.deletandoArquivo.set(null);
    }
  }
}
