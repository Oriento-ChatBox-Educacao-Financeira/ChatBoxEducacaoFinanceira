package com.oriento.api.dto;

import java.time.OffsetDateTime;

/**
 * Item da lista de planilhas importadas (tela de configura\u00e7\u00f5es).
 */
public record PlanilhaResumoResponse(
        String arquivo,
        String hash,
        long linhas,
        OffsetDateTime dataUpload
) {
}
