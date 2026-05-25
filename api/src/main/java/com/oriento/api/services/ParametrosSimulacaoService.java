package com.oriento.api.services;

import com.oriento.api.model.ParametroSimulacao;
import com.oriento.api.model.Simulacao;
import com.oriento.api.repositories.ParametroSimulacaoRepository;
import com.oriento.api.repositories.SimulacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ParametroSimulacaoService {

    private static final Logger logger = LoggerFactory.getLogger(ParametroSimulacaoService.class);

    private final ParametroSimulacaoRepository parametroRepository;
    private final SimulacaoRepository simulacaoRepository;

    public ParametroSimulacaoService(ParametroSimulacaoRepository parametroRepository,
                                     SimulacaoRepository simulacaoRepository) {
        this.parametroRepository = parametroRepository;
        this.simulacaoRepository = simulacaoRepository;
    }

    @Transactional(readOnly = true)
    public List<ParametroSimulacao> listarTodos() {
        logger.debug("Listando todos os parâmetros de simulação");
        return parametroRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ParametroSimulacao> listarPorSimulacao(Integer idSimulacao) {
        logger.debug("Listando parâmetros da simulação ID: {}", idSimulacao);
        return parametroRepository.findBySimulacaoIdSimulacao(idSimulacao);
    }

    @Transactional(readOnly = true)
    public ParametroSimulacao buscarPorId(Integer id) {
        logger.debug("Buscando parâmetro de simulação ID: {}", id);
        return parametroRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Parâmetro de simulação não encontrado. ID: {}", id);
                    return new IllegalArgumentException("Parâmetro de simulação não encontrado com ID: " + id);
                });
    }

    @Transactional
    public ParametroSimulacao criar(ParametroSimulacao parametro, Integer idSimulacao) {
        logger.info("Criando parâmetro para simulação ID: {}", idSimulacao);

        // Validação: variacao_percentual e valor_absoluto não podem ser ambos nulos
        if (parametro.getVariacaoPercentual() == null && parametro.getValorAbsoluto() == null) {
            throw new IllegalArgumentException(
                    "Informe variação percentual ou valor absoluto para o parâmetro.");
        }

        Simulacao simulacao = simulacaoRepository.findById(idSimulacao)
                .orElseThrow(() -> new IllegalArgumentException("Simulação não encontrada com ID: " + idSimulacao));

        parametro.setSimulacao(simulacao);

        ParametroSimulacao salvo = parametroRepository.save(parametro);
        logger.info("Parâmetro de simulação criado com sucesso. ID: {}", salvo.getIdParametro());
        return salvo;
    }

    @Transactional
    public ParametroSimulacao atualizar(Integer id, ParametroSimulacao dadosNovos) {
        logger.info("Atualizando parâmetro de simulação ID: {}", id);

        if (dadosNovos.getVariacaoPercentual() == null && dadosNovos.getValorAbsoluto() == null) {
            throw new IllegalArgumentException(
                    "Informe variação percentual ou valor absoluto para o parâmetro.");
        }

        ParametroSimulacao parametro = parametroRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parâmetro de simulação não encontrado com ID: " + id));

        parametro.setTipo(dadosNovos.getTipo());
        parametro.setVariacaoPercentual(dadosNovos.getVariacaoPercentual());
        parametro.setValorAbsoluto(dadosNovos.getValorAbsoluto());

        ParametroSimulacao atualizado = parametroRepository.save(parametro);
        logger.info("Parâmetro de simulação atualizado com sucesso. ID: {}", atualizado.getIdParametro());
        return atualizado;
    }

    @Transactional
    public void deletar(Integer id) {
        logger.info("Deletando parâmetro de simulação ID: {}", id);
        if (!parametroRepository.existsById(id)) {
            throw new IllegalArgumentException("Parâmetro de simulação não encontrado com ID: " + id);
        }
        parametroRepository.deleteById(id);
        logger.info("Parâmetro de simulação deletado com sucesso. ID: {}", id);
    }
}