package com.oriento.api.dto;

import java.util.List;

/**
 * Resposta padr\u00e3o do upload de planilha. Espelha o contrato que o
 * frontend Angular j\u00e1 esperava do antigo endpoint Supabase.
 */
public record PlanilhaImportResponse(
        String arquivo,
        Integer idEmpresa,
        int linhasImportadas,
        String mensagem,
        List<String> tiposPresentes,
        List<String> tiposFaltantes,
        List<String> avisos
) {
}
