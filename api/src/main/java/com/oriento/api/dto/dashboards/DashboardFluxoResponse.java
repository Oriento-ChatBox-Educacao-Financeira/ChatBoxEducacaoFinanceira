package com.oriento.api.dto.dashboards;

import java.math.BigDecimal;
import java.util.List;

public record DashboardFluxoResponse(
        boolean hasData,
        FluxoKpis kpis,
        List<FluxoMovimentacao> movimentacoes,
        List<FluxoEvolucao> evolucao
) {
    public record FluxoKpis(
            BigDecimal saldoAtual,
            BigDecimal entradas,
            BigDecimal saidas,
            BigDecimal projecao30dias
    ) {}

    public record FluxoMovimentacao(
            String descricao,
            BigDecimal valor,
            String periodo,
            boolean entrada
    ) {}

    public record FluxoEvolucao(
            String mes,
            BigDecimal entrada,
            BigDecimal saida
    ) {}

    public static DashboardFluxoResponse empty() {
        return new DashboardFluxoResponse(false, null, null, null);
    }
}
