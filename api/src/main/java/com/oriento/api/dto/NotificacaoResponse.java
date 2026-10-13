package com.oriento.api.dto;

import com.oriento.api.model.Notificacao;

import java.time.OffsetDateTime;

public record NotificacaoResponse(
        Long idNotificacao,
        String tipo,
        String titulo,
        String mensagem,
        String link,
        String icone,
        boolean lida,
        OffsetDateTime dataCriacao,
        OffsetDateTime dataLeitura
) {
    public static NotificacaoResponse fromEntity(Notificacao n) {
        return new NotificacaoResponse(
                n.getIdNotificacao(),
                n.getTipo(),
                n.getTitulo(),
                n.getMensagem(),
                n.getLink(),
                n.getIcone(),
                n.isLida(),
                n.getDataCriacao(),
                n.getDataLeitura());
    }
}
