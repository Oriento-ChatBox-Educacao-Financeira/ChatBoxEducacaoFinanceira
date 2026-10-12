package com.oriento.api.services;

import com.oriento.api.model.Empresa;
import com.oriento.api.model.IndicadorFinanceiro;
import com.oriento.api.repositories.EmpresaRepository;
import com.oriento.api.repositories.IndicadorFinanceiroRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IndicadorFinanceiroService {

    private static final Logger logger = LoggerFactory.getLogger(IndicadorFinanceiroService.class);

    private final IndicadorFinanceiroRepository indicadorRepository;
    private final EmpresaRepository empresaRepository;

    public IndicadorFinanceiroService(IndicadorFinanceiroRepository indicadorRepository,
                                      EmpresaRepository empresaRepository) {
        this.indicadorRepository = indicadorRepository;
        this.empresaRepository = empresaRepository;
    }

    @Transactional(readOnly = true)
    public List<IndicadorFinanceiro> listarTodos() {
        logger.debug("Listando todos os indicadores financeiros");
        return indicadorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<IndicadorFinanceiro> listarPorEmpresa(Integer idEmpresa) {
        logger.debug("Listando indicadores da empresa ID: {}", idEmpresa);
        return indicadorRepository.findByEmpresaId(idEmpresa);
    }

    @Transactional(readOnly = true)
    public IndicadorFinanceiro buscarPorId(Integer id) {
        logger.debug("Buscando indicador financeiro ID: {}", id);
        return indicadorRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Indicador financeiro não encontrado. ID: {}", id);
                    return new IllegalArgumentException("Indicador financeiro não encontrado com ID: " + id);
                });
    }

    @Transactional
    public IndicadorFinanceiro criar(IndicadorFinanceiro indicador, Integer idEmpresa) {
        logger.info("Criando indicador financeiro para empresa ID: {}", idEmpresa);

        Empresa empresa = empresaRepository.findById(idEmpresa)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada com ID: " + idEmpresa));

        // Valida unicidade (id_empresa, ano_mes) — replicando a @UniqueConstraint do model
        if (indicadorRepository.existsByEmpresaIdAndAnoMes(idEmpresa, indicador.getAnoMes())) {
            throw new IllegalArgumentException(
                    "Já existe indicador cadastrado para esta empresa no período: " + indicador.getAnoMes());
        }

        indicador.setEmpresa(empresa);

        IndicadorFinanceiro salvo = indicadorRepository.save(indicador);
        logger.info("Indicador financeiro criado com sucesso. ID: {}", salvo.getIdIndicador());
        return salvo;
    }

    @Transactional
    public IndicadorFinanceiro atualizar(Integer id, IndicadorFinanceiro dadosNovos) {
        logger.info("Atualizando indicador financeiro ID: {}", id);

        IndicadorFinanceiro indicador = indicadorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Indicador financeiro não encontrado com ID: " + id));

        indicador.setAnoMes(dadosNovos.getAnoMes());
        indicador.setReceitaTotal(dadosNovos.getReceitaTotal());
        indicador.setDespesasTotais(dadosNovos.getDespesasTotais());
        indicador.setLucroLiquido(dadosNovos.getLucroLiquido());
        indicador.setMargemLucro(dadosNovos.getMargemLucro());
        indicador.setFluxoCaixa(dadosNovos.getFluxoCaixa());
        indicador.setExplicacaoAutomatica(dadosNovos.getExplicacaoAutomatica());

        IndicadorFinanceiro atualizado = indicadorRepository.save(indicador);
        logger.info("Indicador financeiro atualizado com sucesso. ID: {}", atualizado.getIdIndicador());
        return atualizado;
    }

    @Transactional
    public void deletar(Integer id) {
        logger.info("Deletando indicador financeiro ID: {}", id);
        if (!indicadorRepository.existsById(id)) {
            throw new IllegalArgumentException("Indicador financeiro não encontrado com ID: " + id);
        }
        indicadorRepository.deleteById(id);
        logger.info("Indicador financeiro deletado com sucesso. ID: {}", id);
    }
}