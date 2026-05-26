package com.oriento.api.dto.dashboards;

import java.math.BigDecimal;
import java.util.List;

public record DashboardBalancoResponse(
        boolean hasData,
        BalancoKpis kpis,
        List<BalancoItem> ativos,
        List<BalancoItem> recursos,
        List<BalancoEvolucao> evolucao
) {
    public record BalancoKpis(
            BigDecimal totalAtivo,
            BigDecimal totalPassivo,
            BigDecimal patrimonioLiquido,
            BigDecimal liquidez
    ) {}

    public record BalancoItem(
            String nome,
            String desc,
            BigDecimal valor
    ) {}

    public record BalancoEvolucao(
            String rotulo,
            BigDecimal patrimonio
    ) {}

    public static DashboardBalancoResponse empty() {
        return new DashboardBalancoResponse(false, null, null, null, null);
    }
}
