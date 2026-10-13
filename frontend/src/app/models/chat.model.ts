export interface ChatMessage {
  sender: 'user' | 'bot';
  text: string;
  timestamp?: Date;
  pending?: boolean;
  streaming?: boolean;
}

export interface AiResponse {
  response: string;
  conversationId?: string;
}

export interface ConversaResumo {
  id: string;
  titulo: string;
  iniciadaEm: string;
  ultimaInteracao: string;
}

export interface MensagemHistoricoConteudo {
  texto: string;
  dados_estruturados?: unknown;
}

export interface MensagemHistorico {
  id?: string;
  conversaId: string;
  ordem: number;
  remetente: 'usuario' | 'ia';
  tipoMensagem?: string;
  conteudo: MensagemHistoricoConteudo;
  dataHora?: string;
}

export interface ContextoFinanceiroFlag {
  geradoEm: string;
  validoAteMs: number;
}

export interface ContextoFinanceiroRefreshResponse {
  geradoEm: string;
  validoPorHoras: number;
}
