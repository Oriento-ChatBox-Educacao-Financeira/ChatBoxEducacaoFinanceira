import { Component, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { MainNavbar } from '../../_components/main-navbar/main-navbar';
import { ChatInfoButton } from '../../_components/chat-info-button/chat-info-button';
import { AuthService } from '../../services/auth.service';
import { PlanilhaService, PlanilhaImportada } from '../../services/planilha.service';
import { LoggerService } from '../../services/logger.service';

@Component({
  selector: 'app-configuracoes',
  standalone: true,
  imports: [CommonModule, FormsModule, MainNavbar, DatePipe, ChatInfoButton],
  templateUrl: './configuracoes.html',
  styleUrl: './configuracoes.css',
})
export class ConfiguracoesPage implements OnInit {
  novaSenha = '';
  confirmaSenha = '';
  senhaMsg = signal<{ tipo: 'sucesso' | 'erro'; texto: string } | null>(null);
  trocandoSenha = signal(false);

  planilhas = signal<PlanilhaImportada[]>([]);
  carregandoPlanilhas = signal(true);
  deletandoArquivo = signal<string | null>(null);

  constructor(
    private auth: AuthService,
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
      this.senhaMsg.set({ tipo: 'erro', texto: 'A senha precisa ter no m\u00ednimo 6 caracteres.' });
      return;
    }
    if (this.novaSenha !== this.confirmaSenha) {
      this.senhaMsg.set({ tipo: 'erro', texto: 'As senhas n\u00e3o coincidem.' });
      return;
    }
    this.trocandoSenha.set(true);
    try {
      await firstValueFrom(this.auth.trocarSenha(this.novaSenha));
      this.novaSenha = '';
      this.confirmaSenha = '';
      this.senhaMsg.set({ tipo: 'sucesso', texto: 'Senha atualizada com sucesso.' });
    } catch (err) {
      this.logger.error('Falha ao trocar senha', err);
      const msg = err instanceof Error ? err.message : 'Falha ao atualizar a senha.';
      this.senhaMsg.set({ tipo: 'erro', texto: msg });
    } finally {
      this.trocandoSenha.set(false);
    }
  }

  perguntasPlanilha(nome: string): string[] {
    return [
      `Explique a planilha "${nome}"`,
      `Identifique problemas em "${nome}"`,
      `Como "${nome}" impacta meu resultado?`,
    ];
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
