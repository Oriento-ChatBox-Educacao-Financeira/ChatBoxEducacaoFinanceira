package com.oriento.api.services;

import com.oriento.api.dto.dashboards.DashboardVisaoGeralResponse;
import com.oriento.api.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gera e mantém em cache um resumo financeiro do usuário, usado como contexto
 * adicional pelo {@link AIService} ao iniciar novas conversas com o LLM.
 *
 * O cache é por usuário e expira após {@link #TTL}. O frontend decide quando
 * acionar o refresh (via {@code POST /api/oriento/contexto/refresh}).
 */
@Service
public class ContextoFinanceiroService {

    private static final Logger logger = LoggerFactory.getLogger(ContextoFinanceiroService.class);

    private static final Duration TTL = Duration.ofHours(24);

    private final DashboardService dashboardService;

    private final Map<UUID, CachedContexto> cache = new ConcurrentHashMap<>();

    public ContextoFinanceiroService(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    public CachedContexto gerar(Usuario usuario) {
        logger.info("Gerando contexto financeiro para usuário {}", usuario.getIdUsuario());
        DashboardVisaoGeralResponse visao = dashboardService.visaoGeral(usuario);
        String texto = formatar(visao);
        CachedContexto registro = new CachedContexto(texto, Instant.now());
        cache.put(usuario.getIdUsuario(), registro);
        logger.debug("Contexto financeiro armazenado em cache (tamanho do texto: {} chars)",
                texto != null ? texto.length() : 0);
        return registro;
    }

    /**
     * Retorna o texto do contexto financeiro do usuário, ou {@code null} se
     * não houver cache válido (TTL expirado ou nunca gerado).
     */
    public String obter(Usuario usuario) {
        CachedContexto registro = cache.get(usuario.getIdUsuario());
        if (registro == null) {
            return null;
        }
        if (Duration.between(registro.geradoEm(), Instant.now()).compareTo(TTL) > 0) {
            logger.debug("Contexto financeiro expirado para usuário {}", usuario.getIdUsuario());
            cache.remove(usuario.getIdUsuario());
            return null;
        }
        return registro.texto();
    }

    public void invalidar(Usuario usuario) {
        cache.remove(usuario.getIdUsuario());
    }

    private String formatar(DashboardVisaoGeralResponse visao) {
        if (visao == null || !visao.hasData() || visao.kpis() == null) {
            return "Nenhum dado financeiro disponível: o usuário ainda não importou planilhas suficientes.";
        }
        DashboardVisaoGeralResponse.VisaoKpis k = visao.kpis();
        NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
        StringBuilder sb = new StringBuilder();
        sb.append("Resumo financeiro atual da empresa do usuário:\n");
        sb.append("- Saldo de caixa: ").append(formatarMoeda(moeda, k.saldoAtual())).append('\n');
        sb.append("- Lucro líquido: ").append(formatarMoeda(moeda, k.lucroLiquido())).append('\n');
        sb.append("- Margem de lucro: ").append(formatarPercentual(k.margem())).append('\n');
        sb.append("- Patrimônio líquido: ").append(formatarMoeda(moeda, k.patrimonioLiquido())).append('\n');
        sb.append("- Liquidez corrente: ").append(formatarNumero(k.liquidez()));
        return sb.toString();
    }

    private String formatarMoeda(NumberFormat fmt, BigDecimal valor) {
        if (valor == null) return "indisponível";
        return fmt.format(valor);
    }

    private String formatarPercentual(BigDecimal valor) {
        if (valor == null) return "indisponível";
        return valor.toPlainString() + "%";
    }

    private String formatarNumero(BigDecimal valor) {
        if (valor == null) return "indisponível";
        return valor.toPlainString();
    }

    public record CachedContexto(String texto, Instant geradoEm) {}
}
