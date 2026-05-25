import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import * as XLSX from 'xlsx';

import { MainNavbar } from '../../_components/main-navbar/main-navbar';
import { PlanilhaService, UploadPlanilhaResponse, PlanilhaDuplicadaError } from '../../services/planilha.service';

type Status = 'idle' | 'uploading' | 'success' | 'error' | 'duplicada';

@Component({
  selector: 'app-upload-planilha',
  standalone: true,
  imports: [CommonModule, MainNavbar],
  templateUrl: './upload-planilha.html',
  styleUrl: './upload-planilha.css',
})
export class UploadPlanilhaComponent {
  status = signal<Status>('idle');
  progresso = signal<number>(0);
  arquivoSelecionado = signal<File | null>(null);
  mensagemErro = signal<string>('');
  resultado = signal<UploadPlanilhaResponse | null>(null);

  readonly extensoesAceitas = ['.csv', '.xlsx', '.xlsm'];
  readonly tamanhoMaximoMb = 15;

  constructor(private planilhaService: PlanilhaService) {}

  onArquivoSelecionado(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.selecionar(file);
  }

  onDrop(evento: DragEvent): void {
    evento.preventDefault();
    const file = evento.dataTransfer?.files?.[0] ?? null;
    this.selecionar(file);
  }

  onDragOver(evento: DragEvent): void {
    evento.preventDefault();
  }

  private selecionar(file: File | null): void {
    this.resultado.set(null);
    this.mensagemErro.set('');
    this.status.set('idle');
    this.progresso.set(0);

    if (!file) return;

    const nome = file.name.toLowerCase();
    const ok = this.extensoesAceitas.some((ext) => nome.endsWith(ext));
    if (!ok) {
      this.mensagemErro.set(`Formato inválido. Aceitos: ${this.extensoesAceitas.join(', ')}`);
      this.status.set('error');
      return;
    }
    if (file.size > this.tamanhoMaximoMb * 1024 * 1024) {
      this.mensagemErro.set(`Arquivo excede ${this.tamanhoMaximoMb}MB.`);
      this.status.set('error');
      return;
    }
    this.arquivoSelecionado.set(file);
  }

  enviar(): void {
    const arq = this.arquivoSelecionado();
    if (!arq) return;

    this.status.set('uploading');
    this.progresso.set(0);
    this.mensagemErro.set('');

    this.planilhaService.upload(arq).subscribe({
      next: (ev) => {
        this.progresso.set(ev.percent);
        if (ev.status === 'done' && ev.response) {
          this.resultado.set(ev.response);
          this.status.set('success');
        }
      },
      error: (err: unknown) => {
        if (err instanceof PlanilhaDuplicadaError) {
          this.status.set('duplicada');
          this.mensagemErro.set(err.mensagem);
          return;
        }
        this.status.set('error');
        this.mensagemErro.set(
          err instanceof Error ? err.message : 'Falha ao processar a planilha.',
        );
      },
    });
  }

  limpar(): void {
    this.arquivoSelecionado.set(null);
    this.status.set('idle');
    this.progresso.set(0);
    this.resultado.set(null);
    this.mensagemErro.set('');
  }

  baixarTemplate(): void {
    const meses = [
      new Date(Date.UTC(2025, 0, 1)),
      new Date(Date.UTC(2025, 1, 1)),
      new Date(Date.UTC(2025, 2, 1)),
      new Date(Date.UTC(2025, 3, 1)),
      new Date(Date.UTC(2025, 4, 1)),
      new Date(Date.UTC(2025, 5, 1)),
    ];

    const dreContas: [string, string][] = [
      ['3.01', '(=) Receita Bruta'],
      ['3.01.01', 'Receita com Mercadorias e Produtos'],
      ['3.01.02', 'Receita com Serviços'],
      ['3.01.03', 'Receita com Locação'],
      ['3.02', '(=) Impostos'],
      ['3.02.01', 'ICMS'],
      ['3.02.02', 'PIS'],
      ['3.02.03', 'COFINS'],
      ['3.02.04', 'ISS'],
      ['3.03', '(-) Deduções da Receita'],
      ['3.04', '(=) Receita Líquida'],
      ['3.05', '(-) Custos'],
      ['3.05.01', 'Custos das Mercadorias e Produtos'],
      ['3.06', '(=) Lucro Bruto'],
      ['3.07', '(-) Despesas Operacionais'],
      ['3.07.01', 'Despesas com Pessoal'],
      ['3.07.02', 'Despesas Administrativas'],
      ['3.07.03', 'Despesas Comerciais'],
      ['3.40', '(=) Lucro Líquido do Exercício'],
    ];

    const bpContas: [string, string][] = [
      ['1', '(=) ATIVO'],
      ['1.01', '(=) Ativo Circulante'],
      ['1.01.01', 'Caixa e Aplicações'],
      ['1.01.02', 'Duplicatas a receber'],
      ['1.01.03', 'Estoques'],
      ['1.02', '(=) Ativo Não Circulante'],
      ['1.02.01', 'Móveis e Utensílios'],
      ['1.02.02', 'Máquinas e Equipamentos'],
      ['2', '(=) PASSIVO'],
      ['2.01', '(=) Passivo Circulante'],
      ['2.01.01', 'Fornecedores'],
      ['2.01.02', 'Obrigações Trabalhistas'],
      ['2.02', '(=) Exigível a Longo Prazo'],
      ['2.03', '(=) Patrimônio Líquido'],
      ['2.03.01', 'Capital Social'],
    ];

    const capitalGiroContas: [string, string][] = [
      ['', '(=) ATIVO OPERACIONAL'],
      ['1.01.02', 'Duplicatas a receber'],
      ['1.01.03', 'Estoques'],
      ['1.01.04', 'Adiantamentos a Funcionários'],
      ['', '(=) PASSIVO OPERACIONAL'],
      ['2.01.01', 'Fornecedores'],
      ['2.01.02', 'Obrigações Trabalhistas'],
      ['', '(=) CAPITAL DE GIRO'],
    ];

    const buildSheet = (titulo: string, header2: string, contas: [string, string][]) => {
      const rows: unknown[][] = [];
      rows.push([titulo, header2, ...meses]);
      rows.push([]);
      for (const [cod, desc] of contas) {
        rows.push([cod || null, desc, null, null, null, null, null, null]);
      }
      return rows;
    };

    const inputRows = [
      ...buildSheet('DRE', 'Descrição das Contas', dreContas),
      [],
      [],
      ...buildSheet('BP', 'Descrição das Contas', bpContas),
    ];
    const capitalGiroRows = [
      [],
      [],
      [],
      ...buildSheet('Capital de Giro', 'Descrição das Contas', capitalGiroContas),
    ];

    const wb = XLSX.utils.book_new();
    const wsInput = XLSX.utils.aoa_to_sheet(inputRows, { cellDates: true });
    const wsCG = XLSX.utils.aoa_to_sheet(capitalGiroRows, { cellDates: true });
    XLSX.utils.book_append_sheet(wb, wsInput, 'Input');
    XLSX.utils.book_append_sheet(wb, wsCG, 'Capital Giro');

    XLSX.writeFile(wb, 'template-oriento.xlsx');
  }
}
