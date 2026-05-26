package com.oriento.api.dto.dashboards;

import java.math.BigDecimal;
import java.util.List;

public record DashboardVisaoGeralResponse(
        boolean hasData,
        VisaoKpis kpis,
        List<VisaoEvolucao> evolucao
) {
    public record VisaoKpis(
            BigDecimal saldoAtual,
            BigDecimal lucroLiquido,
            BigDecimal margem,
            BigDecimal patrimonioLiquido,
            BigDecimal liquidez
    ) {}

    public record VisaoEvolucao(
            String mes,
            BigDecimal receita,
            BigDecimal despesa
    ) {}

    public static DashboardVisaoGeralResponse empty() {
        return new DashboardVisaoGeralResponse(false, null, null);
    }
}
