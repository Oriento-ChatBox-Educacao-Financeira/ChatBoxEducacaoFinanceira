package com.oriento.api.services;

import com.oriento.api.dto.dashboards.DashboardBalancoResponse;
import com.oriento.api.dto.dashboards.DashboardDreResponse;
import com.oriento.api.dto.dashboards.DashboardFluxoResponse;
import com.oriento.api.dto.dashboards.DashboardVisaoGeralResponse;
import com.oriento.api.model.Empresa;
import com.oriento.api.model.LinhaDemonstrativo;
import com.oriento.api.model.Usuario;
import com.oriento.api.repositories.EmpresaRepository;
import com.oriento.api.repositories.LinhaDemonstrativoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Agrega\u00e7\u00f5es sobre {@code linha_demonstrativo} usadas pelos dashboards.
 * Substitui as RPCs {@code dashboard_*} que existiam no Supabase, com a
 * vantagem de viver no mesmo backend que serve a aplica\u00e7\u00e3o.
 *
 * <p>As regras de agrupamento s\u00e3o orientadas pelo template oficial gerado
 * por {@code upload-planilha.ts} (c\u00f3digos hier\u00e1rquicos como 3.01, 3.07,
 * 1.01, 2.03 \u2026). Linhas com c\u00f3digos fora do template ainda s\u00e3o
 * consideradas se a descri\u00e7\u00e3o casar com prefixos conhecidos.
 */
@Service
public class DashboardService {

    private static final DateTimeFormatter MES_LABEL = DateTimeFormatter.ofPattern("MMM/yy", new Locale("pt", "BR"));

    private final EmpresaRepository empresaRepository;
    private final LinhaDemonstrativoRepository linhaRepository;

    public DashboardService(EmpresaRepository empresaRepository,
                            LinhaDemonstrativoRepository linhaRepository) {
        this.empresaRepository = empresaRepository;
        this.linhaRepository = linhaRepository;
    }

    @Transactional(readOnly = true)
    public DashboardDreResponse dre(Usuario usuario) {
        List<LinhaDemonstrativo> linhas = linhasDoUsuarioPorTipo(usuario, "DRE");
        if (linhas.isEmpty()) {
            return DashboardDreResponse.empty();
        }
        BigDecimal receitaBruta = somaPorPrefixo(linhas, "3.01");
        BigDecimal impostos = somaPorPrefixo(linhas, "3.02");
        BigDecimal deducoes = somaPorPrefixo(linhas, "3.03");
        BigDecimal receitaLiquida = somaExataOuCalculo(linhas, "3.04",
                receitaBruta.subtract(impostos).subtract(deducoes));
        BigDecimal custos = somaPorPrefixo(linhas, "3.05");
        BigDecimal lucroBruto = somaExataOuCalculo(linhas, "3.06", receitaLiquida.subtract(custos));
        BigDecimal despesasOperacionais = somaPorPrefixo(linhas, "3.07");
        BigDecimal lucroLiquido = somaExataOuCalculo(linhas, "3.40",
                lucroBruto.subtract(despesasOperacionais));

        BigDecimal margem = BigDecimal.ZERO;
        if (receitaBruta.signum() != 0) {
            margem = lucroLiquido.divide(receitaBruta, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        List<DashboardDreResponse.DreItem> itens = new ArrayList<>();
        itens.add(new DashboardDreResponse.DreItem("(=) Receita Bruta", receitaBruta, true, false, false, false));
        itens.add(new DashboardDreResponse.DreItem("(-) Impostos", impostos.negate(), false, true, false, false));
        itens.add(new DashboardDreResponse.DreItem("(-) Dedu\u00e7\u00f5es", deducoes.negate(), false, true, false, false));
        itens.add(new DashboardDreResponse.DreItem("(=) Receita L\u00edquida", receitaLiquida, true, false, true, false));
        itens.add(new DashboardDreResponse.DreItem("(-) Custos", custos.negate(), false, true, false, false));
        itens.add(new DashboardDreResponse.DreItem("(=) Lucro Bruto", lucroBruto, true, false, true, false));
        itens.add(new DashboardDreResponse.DreItem("(-) Despesas Operacionais", despesasOperacionais.negate(),
                false, true, false, false));
        itens.add(new DashboardDreResponse.DreItem("(=) Lucro L\u00edquido", lucroLiquido, true, false, false, true));

        List<DashboardDreResponse.DreEvolucao> evolucao = evolucaoReceitaDespesa(linhas);

        return new DashboardDreResponse(
                true,
                new DashboardDreResponse.DreKpis(receitaBruta, despesasOperacionais, lucroLiquido, margem),
                itens,
                evolucao);
    }

    @Transactional(readOnly = true)
    public DashboardBalancoResponse balanco(Usuario usuario) {
        List<LinhaDemonstrativo> linhas = linhasDoUsuarioPorTipo(usuario, "BP");
        if (linhas.isEmpty()) {
            return DashboardBalancoResponse.empty();
        }

        LocalDate ultimoPeriodo = linhas.stream()
                .map(LinhaDemonstrativo::getPeriodo)
                .max(Comparator.naturalOrder())
                .orElseThrow();
        List<LinhaDemonstrativo> doPeriodo = linhas.stream()
                .filter(l -> l.getPeriodo().equals(ultimoPeriodo))
                .toList();

        BigDecimal totalAtivo = somaPorPrefixo(doPeriodo, "1");
        BigDecimal totalPassivo = somaPorPrefixo(doPeriodo, "2.01")
                .add(somaPorPrefixo(doPeriodo, "2.02"));
        BigDecimal patrimonioLiquido = somaPorPrefixo(doPeriodo, "2.03");

        BigDecimal ativoCirculante = somaPorPrefixo(doPeriodo, "1.01");
        BigDecimal passivoCirculante = somaPorPrefixo(doPeriodo, "2.01");
        BigDecimal liquidez = passivoCirculante.signum() == 0
                ? null
                : ativoCirculante.divide(passivoCirculante, 4, RoundingMode.HALF_UP);

        List<DashboardBalancoResponse.BalancoItem> ativos = List.of(
                new DashboardBalancoResponse.BalancoItem("Ativo Circulante", "Caixa, contas a receber, estoques",
                        ativoCirculante),
                new DashboardBalancoResponse.BalancoItem("Ativo N\u00e3o Circulante", "Im\u00f3veis, m\u00e1quinas, equipamentos",
                        somaPorPrefixo(doPeriodo, "1.02")));
        List<DashboardBalancoResponse.BalancoItem> recursos = List.of(
                new DashboardBalancoResponse.BalancoItem("Passivo Circulante", "Fornecedores, obriga\u00e7\u00f5es de curto prazo",
                        passivoCirculante),
                new DashboardBalancoResponse.BalancoItem("Exig\u00edvel a Longo Prazo", "Financiamentos e obriga\u00e7\u00f5es futuras",
                        somaPorPrefixo(doPeriodo, "2.02")),
                new DashboardBalancoResponse.BalancoItem("Patrim\u00f4nio L\u00edquido", "Capital social + reservas",
                        patrimonioLiquido));

        Map<LocalDate, BigDecimal> patrimonioPorPeriodo = linhas.stream()
                .filter(l -> codigoComecaCom(l.getCodigoConta(), "2.03"))
                .collect(Collectors.groupingBy(LinhaDemonstrativo::getPeriodo,
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, LinhaDemonstrativo::getValor, BigDecimal::add)));
        List<DashboardBalancoResponse.BalancoEvolucao> evolucao = patrimonioPorPeriodo.entrySet().stream()
                .map(e -> new DashboardBalancoResponse.BalancoEvolucao(formatarMes(e.getKey()), e.getValue()))
                .toList();

        return new DashboardBalancoResponse(
                true,
                new DashboardBalancoResponse.BalancoKpis(totalAtivo, totalPassivo, patrimonioLiquido, liquidez),
                ativos,
                recursos,
                evolucao);
    }

    @Transactional(readOnly = true)
    public DashboardFluxoResponse fluxoCaixa(Usuario usuario) {
        List<LinhaDemonstrativo> linhas = linhasDoUsuarioPorTipos(usuario, List.of("FLUXO_CAIXA", "OUTRO"));
        if (linhas.isEmpty()) {
            return DashboardFluxoResponse.empty();
        }

        BigDecimal entradas = linhas.stream()
                .filter(l -> l.getValor().signum() > 0)
                .map(LinhaDemonstrativo::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saidas = linhas.stream()
                .filter(l -> l.getValor().signum() < 0)
                .map(LinhaDemonstrativo::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .abs();
        BigDecimal saldoAtual = entradas.subtract(saidas);

        Map<LocalDate, BigDecimal[]> porMes = new TreeMap<>();
        for (LinhaDemonstrativo l : linhas) {
            porMes.computeIfAbsent(l.getPeriodo(), p -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal[] arr = porMes.get(l.getPeriodo());
            if (l.getValor().signum() >= 0) {
                arr[0] = arr[0].add(l.getValor());
            } else {
                arr[1] = arr[1].add(l.getValor().abs());
            }
        }
        List<DashboardFluxoResponse.FluxoEvolucao> evolucao = porMes.entrySet().stream()
                .map(e -> new DashboardFluxoResponse.FluxoEvolucao(formatarMes(e.getKey()), e.getValue()[0], e.getValue()[1]))
                .toList();

        // M\u00e9dia mensal das entradas-sa\u00eddas como proje\u00e7\u00e3o simplificada
        BigDecimal projecao = BigDecimal.ZERO;
        if (!porMes.isEmpty()) {
            BigDecimal soma = porMes.values().stream()
                    .map(arr -> arr[0].subtract(arr[1]))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            projecao = soma.divide(BigDecimal.valueOf(porMes.size()), 2, RoundingMode.HALF_UP);
        }

        List<DashboardFluxoResponse.FluxoMovimentacao> movimentacoes = linhas.stream()
                .sorted(Comparator.comparing(LinhaDemonstrativo::getPeriodo).reversed())
                .limit(20)
                .map(l -> new DashboardFluxoResponse.FluxoMovimentacao(
                        l.getDescricao(),
                        l.getValor().abs(),
                        formatarMes(l.getPeriodo()),
                        l.getValor().signum() >= 0))
                .toList();

        return new DashboardFluxoResponse(
                true,
                new DashboardFluxoResponse.FluxoKpis(saldoAtual, entradas, saidas, projecao),
                movimentacoes,
                evolucao);
    }

    @Transactional(readOnly = true)
    public DashboardVisaoGeralResponse visaoGeral(Usuario usuario) {
        DashboardDreResponse dre = dre(usuario);
        DashboardBalancoResponse balanco = balanco(usuario);
        DashboardFluxoResponse fluxo = fluxoCaixa(usuario);

        if (!dre.hasData() && !balanco.hasData() && !fluxo.hasData()) {
            return DashboardVisaoGeralResponse.empty();
        }

        BigDecimal saldoAtual = fluxo.hasData() ? fluxo.kpis().saldoAtual() : BigDecimal.ZERO;
        BigDecimal lucroLiquido = dre.hasData() ? dre.kpis().lucroLiquido() : BigDecimal.ZERO;
        BigDecimal margem = dre.hasData() ? dre.kpis().margemLucro() : BigDecimal.ZERO;
        BigDecimal patrimonioLiquido = balanco.hasData() ? balanco.kpis().patrimonioLiquido() : BigDecimal.ZERO;
        BigDecimal liquidez = balanco.hasData() && balanco.kpis().liquidez() != null
                ? balanco.kpis().liquidez()
                : BigDecimal.ZERO;

        List<DashboardVisaoGeralResponse.VisaoEvolucao> evolucao = dre.hasData() && dre.evolucao() != null
                ? dre.evolucao().stream()
                .map(e -> new DashboardVisaoGeralResponse.VisaoEvolucao(e.mes(), e.receita(), e.despesa()))
                .toList()
                : List.of();

        return new DashboardVisaoGeralResponse(
                true,
                new DashboardVisaoGeralResponse.VisaoKpis(saldoAtual, lucroLiquido, margem, patrimonioLiquido, liquidez),
                evolucao);
    }

    // ============================================================
    // Helpers
    // ============================================================

    private List<LinhaDemonstrativo> linhasDoUsuarioPorTipo(Usuario usuario, String tipo) {
        return linhasDoUsuarioPorTipos(usuario, List.of(tipo));
    }

    private List<LinhaDemonstrativo> linhasDoUsuarioPorTipos(Usuario usuario, List<String> tipos) {
        Optional<Empresa> empresaOpt = empresaRepository.findByUsuario_IdUsuario(usuario.getIdUsuario());
        if (empresaOpt.isEmpty()) {
            return List.of();
        }
        List<LinhaDemonstrativo> linhas = linhaRepository
                .findByEmpresa_IdOrderByPeriodoAsc(empresaOpt.get().getId());
        return linhas.stream()
                .filter(l -> tipos.contains(l.getTipo()))
                .toList();
    }

    private BigDecimal somaPorPrefixo(List<LinhaDemonstrativo> linhas, String prefixo) {
        return linhas.stream()
                .filter(l -> codigoComecaCom(l.getCodigoConta(), prefixo))
                .map(LinhaDemonstrativo::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somaExataOuCalculo(List<LinhaDemonstrativo> linhas, String codigo, BigDecimal fallback) {
        BigDecimal exata = linhas.stream()
                .filter(l -> codigo.equals(l.getCodigoConta()))
                .map(LinhaDemonstrativo::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return exata.signum() == 0 ? fallback : exata;
    }

    private boolean codigoComecaCom(String codigo, String prefixo) {
        if (codigo == null) return false;
        return codigo.equals(prefixo) || codigo.startsWith(prefixo + ".");
    }

    private List<DashboardDreResponse.DreEvolucao> evolucaoReceitaDespesa(List<LinhaDemonstrativo> linhas) {
        Map<LocalDate, BigDecimal[]> porMes = new TreeMap<>();
        for (LinhaDemonstrativo l : linhas) {
            String cod = l.getCodigoConta();
            if (cod == null) continue;
            porMes.computeIfAbsent(l.getPeriodo(), p -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal[] arr = porMes.get(l.getPeriodo());
            if (cod.startsWith("3.01")) {
                arr[0] = arr[0].add(l.getValor());
            } else if (cod.startsWith("3.05") || cod.startsWith("3.07")) {
                arr[1] = arr[1].add(l.getValor());
            }
        }
        return porMes.entrySet().stream()
                .map(e -> new DashboardDreResponse.DreEvolucao(
                        formatarMes(e.getKey()), e.getValue()[0], e.getValue()[1]))
                .toList();
    }

    private String formatarMes(LocalDate data) {
        return data.format(MES_LABEL);
    }
}
