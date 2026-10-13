package com.oriento.api.dto.dashboards;

import java.math.BigDecimal;
import java.util.List;

public record DashboardDreResponse(
        boolean hasData,
        DreKpis kpis,
        List<DreItem> itens,
        List<DreEvolucao> evolucao
) {
    public record DreKpis(
            BigDecimal receitaBruta,
            BigDecimal despOperacionais,
            BigDecimal lucroLiquido,
            BigDecimal margemLucro
    ) {}

    public record DreItem(
            String label,
            BigDecimal valor,
            Boolean bold,
            Boolean sub,
            Boolean blue,
            Boolean total
    ) {}

    public record DreEvolucao(
            String mes,
            BigDecimal receita,
            BigDecimal despesa
    ) {}

    public static DashboardDreResponse empty() {
        return new DashboardDreResponse(false, null, null, null);
    }
}
