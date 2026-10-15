package com.oriento.api.dto;

import com.oriento.api.model.IndicadorFinanceiro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EmpresaIndicadoresResponse(Integer empresaId, List<Indicador> indicadores) {
    public record Indicador(Integer idIndicador, LocalDate anoMes, String receitaTotal,
                            String despesasTotais, String lucroLiquido, String margemLucro, String fluxoCaixa) {
        public static Indicador from(IndicadorFinanceiro source) {
            return new Indicador(source.getIdIndicador(), source.getAnoMes(),
                    decimal(source.getReceitaTotal()), decimal(source.getDespesasTotais()),
                    decimal(source.getLucroLiquido()), decimal(source.getMargemLucro()),
                    decimal(source.getFluxoCaixa()));
        }

        private static String decimal(BigDecimal value) {
            return value == null ? null : value.toPlainString();
        }
    }
}
