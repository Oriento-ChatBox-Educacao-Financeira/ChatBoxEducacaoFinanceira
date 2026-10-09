package com.oriento.api.services;

import com.oriento.api.dto.PlanilhaImportResponse;
import com.oriento.api.dto.PlanilhaResumoResponse;
import com.oriento.api.exception.PlanilhaDuplicadaException;
import com.oriento.api.model.Empresa;
import com.oriento.api.model.LinhaDemonstrativo;
import com.oriento.api.model.Usuario;
import com.oriento.api.repositories.EmpresaRepository;
import com.oriento.api.repositories.LinhaDemonstrativoRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Importa\u00e7\u00e3o de planilhas (XLSX/XLSM/CSV) para
 * {@code linha_demonstrativo}. Substitui a edge function Supabase
 * {@code parse-planilha}, mantendo o mesmo contrato com o frontend:
 * detecta hash duplicado, deduplica por chave natural antes do save e
 * gera notifica\u00e7\u00e3o autom\u00e1tica quando faltam tipos de demonstrativo.
 *
 * <p>Formatos suportados:
 * <ul>
 *   <li><b>Wide</b> (template oficial gerado pelo front): marcadores
 *       {@code DRE}, {@code BP}, {@code Capital de Giro}, {@code Fluxo de Caixa}
 *       seguidos pelos meses na linha do cabe\u00e7alho. Cada linha de conta
 *       gera uma {@code linha_demonstrativo} por m\u00eas preenchido.</li>
 *   <li><b>CSV flat</b>: header
 *       {@code tipo,codigo_conta,descricao,periodo,valor}.</li>
 * </ul>
 */
@Service
public class PlanilhaService {

    private static final Logger logger = LoggerFactory.getLogger(PlanilhaService.class);

    // Valores aceitos pela constraint Postgres
    // {@code linha_demonstrativo_tipo_check}: ('DRE','BP','FLUXO_CAIXA','OUTRO').
    // "Capital de Giro" cai em OUTRO por enquanto (a constraint do banco
    // n\u00e3o possui tipo dedicado); o DashboardService agrega ambos no
    // dashboard de fluxo de caixa.
    private static final Set<String> TIPOS_ESPERADOS = Set.of("DRE", "BP", "FLUXO_CAIXA");

    private static final Map<String, String> MARCADORES = Map.of(
            "dre", "DRE",
            "bp", "BP",
            "balan\u00e7o patrimonial", "BP",
            "balanco patrimonial", "BP",
            "capital de giro", "OUTRO",
            "capital giro", "OUTRO",
            "fluxo de caixa", "FLUXO_CAIXA",
            "fluxo caixa", "FLUXO_CAIXA"
    );

    private final LinhaDemonstrativoRepository linhaRepository;
    private final EmpresaRepository empresaRepository;
    private final NotificacaoService notificacaoService;

    public PlanilhaService(LinhaDemonstrativoRepository linhaRepository,
                           EmpresaRepository empresaRepository,
                           NotificacaoService notificacaoService) {
        this.linhaRepository = linhaRepository;
        this.empresaRepository = empresaRepository;
        this.notificacaoService = notificacaoService;
    }

    @Transactional
    public PlanilhaImportResponse importar(Usuario usuario, MultipartFile file) {
        Empresa empresa = empresaRepository.findByUsuario_IdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Empresa do usu\u00e1rio n\u00e3o encontrada"));

        String nomeArquivo = nomeArquivoSeguro(file.getOriginalFilename());
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falha ao ler arquivo");
        }
        String hash = sha256(bytes);

        if (linhaRepository.existsByEmpresa_IdAndHashArquivo(empresa.getId(), hash)) {
            throw new PlanilhaDuplicadaException(
                    "Esta planilha j\u00e1 foi importada anteriormente.",
                    nomeArquivo);
        }

        List<LinhaDemonstrativo> linhas;
        List<String> avisos = new ArrayList<>();
        String nomeLower = nomeArquivo.toLowerCase(Locale.ROOT);
        if (nomeLower.endsWith(".csv")) {
            linhas = parseCsv(bytes, empresa, avisos);
        } else if (nomeLower.endsWith(".xlsx") || nomeLower.endsWith(".xlsm")) {
            linhas = parseXlsx(bytes, empresa, avisos);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Formato n\u00e3o suportado. Use .csv, .xlsx ou .xlsm.");
        }

        if (linhas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Nenhuma linha v\u00e1lida encontrada na planilha.");
        }

        // Dedup interno por chave natural (tipo, codigo, descricao, periodo)
        Map<String, LinhaDemonstrativo> dedup = new HashMap<>();
        for (LinhaDemonstrativo l : linhas) {
            l.setArquivoOrigem(nomeArquivo);
            l.setHashArquivo(hash);
            dedup.merge(chaveNatural(l), l, (a, b) -> b);
        }

        List<LinhaDemonstrativo> finais = new ArrayList<>(dedup.values());
        linhaRepository.saveAll(finais);
        logger.info("Importa\u00e7\u00e3o conclu\u00edda. Empresa={}, arquivo={}, linhas={}",
                empresa.getId(), nomeArquivo, finais.size());

        Set<String> tiposPresentes = new HashSet<>();
        for (LinhaDemonstrativo l : finais) {
            tiposPresentes.add(l.getTipo());
        }
        Set<String> faltantes = new HashSet<>(TIPOS_ESPERADOS);
        faltantes.removeAll(tiposPresentes);

        if (!faltantes.isEmpty()) {
            gerarNotificacaoIncompleta(usuario.getIdUsuario(), nomeArquivo, faltantes);
        }

        return new PlanilhaImportResponse(
                nomeArquivo,
                empresa.getId(),
                finais.size(),
                faltantes.isEmpty()
                        ? "Planilha importada com sucesso."
                        : "Planilha importada parcialmente. Tipos faltantes: " + String.join(", ", faltantes),
                new ArrayList<>(tiposPresentes),
                new ArrayList<>(faltantes),
                avisos);
    }

    @Transactional(readOnly = true)
    public List<PlanilhaResumoResponse> listarPorUsuario(Usuario usuario) {
        Empresa empresa = empresaRepository.findByUsuario_IdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Empresa do usu\u00e1rio n\u00e3o encontrada"));
        return linhaRepository.resumirPorArquivo(empresa.getId()).stream()
                .map(p -> new PlanilhaResumoResponse(p.getArquivo(), p.getHash(), p.getLinhas(), p.getDataUpload()))
                .toList();
    }

    @Transactional
    public int deletar(Usuario usuario, String arquivo) {
        Empresa empresa = empresaRepository.findByUsuario_IdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Empresa do usu\u00e1rio n\u00e3o encontrada"));
        return linhaRepository.deleteByEmpresaAndArquivo(empresa.getId(), arquivo);
    }

    // ============================================================
    // XLSX
    // ============================================================

    private List<LinhaDemonstrativo> parseXlsx(byte[] bytes, Empresa empresa, List<String> avisos) {
        List<LinhaDemonstrativo> linhas = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            DataFormatter fmt = new DataFormatter(new Locale("pt", "BR"));
            for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                Sheet sheet = wb.getSheetAt(s);
                processarSheet(sheet, fmt, empresa, linhas, avisos);
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Falha ao ler arquivo Excel: " + ex.getMessage());
        }
        return linhas;
    }

    private void processarSheet(Sheet sheet, DataFormatter fmt, Empresa empresa,
                                List<LinhaDemonstrativo> destino, List<String> avisos) {
        String tipoCorrente = null;
        List<LocalDate> mesesCorrentes = new ArrayList<>();
        int colunaDescricao = -1;
        int linhasVaziasConsecutivas = 0;

        int rowMax = sheet.getLastRowNum();
        for (int r = sheet.getFirstRowNum(); r <= rowMax; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                if (++linhasVaziasConsecutivas >= 2) {
                    tipoCorrente = null;
                    mesesCorrentes.clear();
                }
                continue;
            }

            String primeiraCelula = textoDe(row.getCell(0), fmt);
            String tipoMarcador = MARCADORES.get(primeiraCelula.toLowerCase(Locale.ROOT).trim());

            if (tipoMarcador != null) {
                tipoCorrente = tipoMarcador;
                mesesCorrentes = extrairMeses(row, fmt);
                colunaDescricao = mesesCorrentes.isEmpty() ? -1 : 1;
                linhasVaziasConsecutivas = 0;
                continue;
            }

            if (tipoCorrente == null || mesesCorrentes.isEmpty()) {
                continue;
            }

            boolean linhaVazia = isLinhaVazia(row, fmt);
            if (linhaVazia) {
                if (++linhasVaziasConsecutivas >= 2) {
                    tipoCorrente = null;
                    mesesCorrentes.clear();
                }
                continue;
            }
            linhasVaziasConsecutivas = 0;

            String codigo = textoDe(row.getCell(0), fmt);
            String descricao = textoDe(row.getCell(colunaDescricao), fmt);
            if (descricao.isBlank()) {
                continue;
            }

            int primeiraColValor = colunaDescricao + 1;
            for (int i = 0; i < mesesCorrentes.size(); i++) {
                Cell celulaValor = row.getCell(primeiraColValor + i);
                BigDecimal valor = numero(celulaValor);
                if (valor == null) {
                    continue;
                }
                LinhaDemonstrativo l = new LinhaDemonstrativo();
                l.setEmpresa(empresa);
                l.setTipo(tipoCorrente);
                l.setCodigoConta(codigo.isBlank() ? null : codigo);
                l.setDescricao(descricao);
                l.setPeriodo(mesesCorrentes.get(i));
                l.setGranularidade("MENSAL");
                l.setValor(valor);
                destino.add(l);
            }
        }
    }

    private List<LocalDate> extrairMeses(Row headerRow, DataFormatter fmt) {
        List<LocalDate> meses = new ArrayList<>();
        for (int c = 2; c <= headerRow.getLastCellNum(); c++) {
            Cell celula = headerRow.getCell(c);
            if (celula == null) continue;
            LocalDate periodo = aMes(celula, fmt);
            if (periodo == null) continue;
            meses.add(periodo);
        }
        return meses;
    }

    private LocalDate aMes(Cell celula, DataFormatter fmt) {
        if (celula.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(celula)) {
            return celula.getDateCellValue()
                    .toInstant().atZone(ZoneId.of("UTC")).toLocalDate().withDayOfMonth(1);
        }
        String texto = textoDe(celula, fmt).trim();
        if (texto.isEmpty()) return null;
        for (DateTimeFormatter f : List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM"))) {
            try {
                LocalDate d = LocalDate.parse(texto, f);
                return d.withDayOfMonth(1);
            } catch (Exception ignored) {
                // try next
            }
        }
        try {
            YearMonth ym = YearMonth.parse(texto);
            return ym.atDay(1);
        } catch (Exception ignored) {
            return null;
        }
    }

    private BigDecimal numero(Cell c) {
        if (c == null) return null;
        switch (c.getCellType()) {
            case NUMERIC -> {
                return BigDecimal.valueOf(c.getNumericCellValue());
            }
            case STRING -> {
                String s = c.getStringCellValue().trim();
                if (s.isEmpty()) return null;
                s = s.replace(".", "").replace(",", ".");
                try {
                    return new BigDecimal(s);
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
            case FORMULA -> {
                try {
                    return BigDecimal.valueOf(c.getNumericCellValue());
                } catch (IllegalStateException ex) {
                    return null;
                }
            }
            default -> {
                return null;
            }
        }
    }

    private boolean isLinhaVazia(Row row, DataFormatter fmt) {
        for (int c = row.getFirstCellNum(); c <= row.getLastCellNum(); c++) {
            if (!textoDe(row.getCell(c), fmt).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String textoDe(Cell cell, DataFormatter fmt) {
        if (cell == null) return "";
        return fmt.formatCellValue(cell).trim();
    }

    // ============================================================
    // CSV
    // ============================================================

    private List<LinhaDemonstrativo> parseCsv(byte[] bytes, Empresa empresa, List<String> avisos) {
        List<LinhaDemonstrativo> linhas = new ArrayList<>();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build();
        try (CSVParser parser = CSVParser.parse(
                new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8),
                format)) {
            List<String> headers = parser.getHeaderNames();
            if (!headers.containsAll(Arrays.asList("tipo", "descricao", "periodo", "valor"))) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "CSV deve conter ao menos as colunas: tipo, descricao, periodo, valor.");
            }
            for (CSVRecord rec : parser) {
                String tipo = MARCADORES.getOrDefault(rec.get("tipo").toLowerCase(Locale.ROOT), rec.get("tipo"));
                String descricao = rec.get("descricao");
                if (descricao.isBlank()) continue;

                LocalDate periodo;
                try {
                    String p = rec.get("periodo");
                    periodo = LocalDate.parse(p.length() == 7 ? p + "-01" : p);
                } catch (Exception ex) {
                    avisos.add("Per\u00edodo inv\u00e1lido ignorado na linha " + rec.getRecordNumber());
                    continue;
                }

                BigDecimal valor;
                try {
                    valor = new BigDecimal(rec.get("valor").replace(".", "").replace(",", "."));
                } catch (NumberFormatException ex) {
                    avisos.add("Valor inv\u00e1lido ignorado na linha " + rec.getRecordNumber());
                    continue;
                }

                LinhaDemonstrativo l = new LinhaDemonstrativo();
                l.setEmpresa(empresa);
                l.setTipo(tipo);
                l.setCodigoConta(headers.contains("codigo_conta") ? blankToNull(rec.get("codigo_conta")) : null);
                l.setDescricao(descricao);
                l.setPeriodo(periodo);
                l.setGranularidade("MENSAL");
                l.setValor(valor);
                linhas.add(l);
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Falha ao ler CSV: " + ex.getMessage());
        }
        return linhas;
    }

    private String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    // ============================================================
    // Utilit\u00e1rios
    // ============================================================

    private String nomeArquivoSeguro(String original) {
        if (original == null || original.isBlank()) {
            return "planilha-" + System.currentTimeMillis() + ".xlsx";
        }
        return original.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indispon\u00edvel", ex);
        }
    }

    private String chaveNatural(LinhaDemonstrativo l) {
        return String.join("|",
                l.getTipo(),
                l.getCodigoConta() == null ? "" : l.getCodigoConta(),
                l.getDescricao(),
                l.getPeriodo().toString());
    }

    private void gerarNotificacaoIncompleta(UUID idUsuario, String arquivo, Set<String> faltantes) {
        try {
            String lista = String.join(", ", faltantes);
            notificacaoService.criar(
                    idUsuario,
                    "PLANILHA_INCOMPLETA",
                    "Planilha importada parcialmente",
                    "O arquivo \"" + arquivo + "\" foi importado, mas n\u00e3o cont\u00e9m: " + lista
                            + ". Dashboards relacionados ficar\u00e3o zerados at\u00e9 voc\u00ea enviar uma planilha completa.",
                    "/upload-planilha",
                    "ph-warning");
        } catch (RuntimeException ex) {
            logger.warn("Falha ao gerar notifica\u00e7\u00e3o de planilha incompleta", ex);
        }
    }
}
