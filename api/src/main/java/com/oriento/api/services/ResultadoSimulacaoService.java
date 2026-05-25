package com.oriento.api.services;

import com.oriento.api.model.ResultadoSimulacao;
import com.oriento.api.model.Simulacao;
import com.oriento.api.repositories.ResultadoSimulacaoRepository;
import com.oriento.api.repositories.SimulacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ResultadoSimulacaoService {

    private static final Logger logger = LoggerFactory.getLogger(ResultadoSimulacaoService.class);

    private final ResultadoSimulacaoRepository resultadoRepository;
    private final SimulacaoRepository simulacaoRepository;

    public ResultadoSimulacaoService(ResultadoSimulacaoRepository resultadoRepository,
                                     SimulacaoRepository simulacaoRepository) {
        this.resultadoRepository = resultadoRepository;
        this.simulacaoRepository = simulacaoRepository;
    }

    @Transactional(readOnly = true)
    public List<ResultadoSimulacao> listarTodos() {
        logger.debug("Listando todos os resultados de simulação");
        return resultadoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ResultadoSimulacao buscarPorId(Integer id) {
        logger.debug("Buscando resultado de simulação ID: {}", id);
        return resultadoRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Resultado de simulação não encontrado. ID: {}", id);
                    return new IllegalArgumentException("Resultado de simulação não encontrado com ID: " + id);
                });
    }

    @Transactional(readOnly = true)
    public ResultadoSimulacao buscarPorSimulacao(Integer idSimulacao) {
        logger.debug("Buscando resultado da simulação ID: {}", idSimulacao);
        // ResultadoSimulacao tem @OneToOne com Simulacao
        return resultadoRepository.findBySimulacaoIdSimulacao(idSimulacao)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Resultado não encontrado para a simulação ID: " + idSimulacao));
    }

    @Transactional
    public ResultadoSimulacao criar(ResultadoSimulacao resultado, Integer idSimulacao) {
        logger.info("Criando resultado para simulação ID: {}", idSimulacao);

        Simulacao simulacao = simulacaoRepository.findById(idSimulacao)
                .orElseThrow(() -> new IllegalArgumentException("Simulação não encontrada com ID: " + idSimulacao));

        // Garante que uma simulação não tenha dois resultados (OneToOne)
        if (resultadoRepository.findBySimulacaoIdSimulacao(idSimulacao).isPresent()) {
            throw new IllegalArgumentException(
                    "Já existe resultado cadastrado para a simulação ID: " + idSimulacao);
        }

        resultado.setSimulacao(simulacao);

        ResultadoSimulacao salvo = resultadoRepository.save(resultado);
        logger.info("Resultado de simulação criado com sucesso. ID: {}", salvo.getIdResultado());
        return salvo;
    }

    @Transactional
    public ResultadoSimulacao atualizar(Integer id, ResultadoSimulacao dadosNovos) {
        logger.info("Atualizando resultado de simulação ID: {}", id);

        ResultadoSimulacao resultado = resultadoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resultado de simulação não encontrado com ID: " + id));

        resultado.setLucroProjetado(dadosNovos.getLucroProjetado());
        resultado.setMargemProjetada(dadosNovos.getMargemProjetada());
        resultado.setFluxoCaixaProjetado(dadosNovos.getFluxoCaixaProjetado());
        resultado.setGraficoDados(dadosNovos.getGraficoDados());

        ResultadoSimulacao atualizado = resultadoRepository.save(resultado);
        logger.info("Resultado de simulação atualizado com sucesso. ID: {}", atualizado.getIdResultado());
        return atualizado;
    }

    @Transactional
    public void deletar(Integer id) {
        logger.info("Deletando resultado de simulação ID: {}", id);
        if (!resultadoRepository.existsById(id)) {
            throw new IllegalArgumentException("Resultado de simulação não encontrado com ID: " + id);
        }
        resultadoRepository.deleteById(id);
        logger.info("Resultado de simulação deletado com sucesso. ID: {}", id);
    }
}