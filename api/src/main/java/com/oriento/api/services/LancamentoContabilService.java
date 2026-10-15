package com.oriento.api.services;

import com.oriento.api.model.Empresa;
import com.oriento.api.model.LancamentoContabil;
import com.oriento.api.model.Simulacao;
import com.oriento.api.repositories.EmpresaRepository;
import com.oriento.api.repositories.LancamentoContabilRepository;
import com.oriento.api.repositories.SimulacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LancamentoContabilService {

    private static final Logger logger = LoggerFactory.getLogger(LancamentoContabilService.class);

    private final LancamentoContabilRepository lancamentoRepository;
    private final EmpresaRepository empresaRepository;
    private final SimulacaoRepository simulacaoRepository;

    public LancamentoContabilService(LancamentoContabilRepository lancamentoRepository,
                                     EmpresaRepository empresaRepository,
                                     SimulacaoRepository simulacaoRepository) {
        this.lancamentoRepository = lancamentoRepository;
        this.empresaRepository = empresaRepository;
        this.simulacaoRepository = simulacaoRepository;
    }

    /**
     * Lista lançamentos contábeis pertencentes à empresa autenticada (anti-IDOR).
     */
    @Transactional(readOnly = true)
    public List<LancamentoContabil> listarPorEmpresa(Integer idEmpresa) {
        logger.debug("Listando lançamentos da empresa ID: {}", idEmpresa);
        return lancamentoRepository.buscarPorEmpresaId(idEmpresa);
    }

    /**
     * Busca lançamento contábil verificando se pertence à empresa autenticada (anti-IDOR).
     */
    @Transactional(readOnly = true)
    public LancamentoContabil buscarPorId(Integer id, Integer idEmpresa) {
        logger.debug("Buscando lançamento contábil ID: {} para empresa ID: {}", id, idEmpresa);
        return lancamentoRepository.findByIdLancamentoAndEmpresaId(id, idEmpresa)
                .orElseThrow(() -> {
                    logger.warn("Lançamento não encontrado ou não pertence à empresa. ID: {}", id);
                    return new IllegalArgumentException("Lançamento não encontrado com ID: " + id);
                });
    }

    @Transactional
    public LancamentoContabil criar(LancamentoContabil lancamento, Integer idEmpresa, Integer idSimulacao) {
        logger.info("Criando lançamento contábil para empresa ID: {}", idEmpresa);

        Empresa empresa = empresaRepository.findById(idEmpresa)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada com ID: " + idEmpresa));

        lancamento.setId_empresa(empresa);

        // Simulação é opcional — só vincula se idSimulacao for informado
        if (idSimulacao != null) {
            Simulacao simulacao = simulacaoRepository.findById(idSimulacao)
                    .orElseThrow(() -> new IllegalArgumentException("Simulação não encontrada com ID: " + idSimulacao));
            lancamento.setId_simualcao(simulacao);
        }

        LancamentoContabil salvo = lancamentoRepository.save(lancamento);
        logger.info("Lançamento contábil criado com sucesso. ID: {}", salvo.getId_lancamento());
        return salvo;
    }

    @Transactional
    public LancamentoContabil atualizar(Integer id, LancamentoContabil dadosNovos, Integer idEmpresa) {
        logger.info("Atualizando lançamento contábil ID: {} para empresa ID: {}", id, idEmpresa);

        LancamentoContabil lancamento = lancamentoRepository.findByIdLancamentoAndEmpresaId(id, idEmpresa)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado com ID: " + id));

        lancamento.setData(dadosNovos.getData());
        lancamento.setDescricao(dadosNovos.getDescricao());
        lancamento.setDocumento(dadosNovos.getDocumento());
        lancamento.setOrigem(dadosNovos.getOrigem());

        LancamentoContabil atualizado = lancamentoRepository.save(lancamento);
        logger.info("Lançamento contábil atualizado com sucesso. ID: {}", atualizado.getId_lancamento());
        return atualizado;
    }

    @Transactional
    public void deletar(Integer id, Integer idEmpresa) {
        logger.info("Deletando lançamento contábil ID: {} para empresa ID: {}", id, idEmpresa);
        LancamentoContabil lancamento = lancamentoRepository.findByIdLancamentoAndEmpresaId(id, idEmpresa)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado com ID: " + id));
        lancamentoRepository.delete(lancamento);
        logger.info("Lançamento contábil deletado com sucesso. ID: {}", id);
    }
}