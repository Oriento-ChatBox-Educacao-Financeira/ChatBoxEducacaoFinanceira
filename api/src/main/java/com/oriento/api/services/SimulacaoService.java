package com.oriento.api.services;

import com.oriento.api.model.Empresa;
import com.oriento.api.model.Simulacao;
import com.oriento.api.repositories.EmpresaRepository;
import com.oriento.api.repositories.SimulacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SimulacaoService {

    private static final Logger logger = LoggerFactory.getLogger(SimulacaoService.class);

    private final SimulacaoRepository simulacaoRepository;
    private final EmpresaRepository empresaRepository;

    public SimulacaoService(SimulacaoRepository simulacaoRepository,
                            EmpresaRepository empresaRepository) {
        this.simulacaoRepository = simulacaoRepository;
        this.empresaRepository = empresaRepository;
    }

    @Transactional(readOnly = true)
    public List<Simulacao> listarTodas() {
        logger.debug("Listando todas as simulações");
        return simulacaoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Simulacao> listarPorEmpresa(Integer idEmpresa) {
        logger.debug("Listando simulações da empresa ID: {}", idEmpresa);
        return simulacaoRepository.findByEmpresaId(idEmpresa);
    }

    @Transactional(readOnly = true)
    public Simulacao buscarPorId(Integer id) {
        logger.debug("Buscando simulação ID: {}", id);
        return simulacaoRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Simulação não encontrada. ID: {}", id);
                    return new IllegalArgumentException("Simulação não encontrada com ID: " + id);
                });
    }

    @Transactional
    public Simulacao criar(Simulacao simulacao, Integer idEmpresa, UUID idUsuario) {
        logger.info("Criando simulação para empresa ID: {}, usuário ID: {}", idEmpresa, idUsuario);

        Empresa empresa = empresaRepository.findById(idEmpresa)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada com ID: " + idEmpresa));

        simulacao.setEmpresa(empresa);
        // idUsuario é armazenado como UUID diretamente no model (sem FK JPA para Usuario)
        simulacao.setIdUsuario(idUsuario);

        Simulacao salva = simulacaoRepository.save(simulacao);
        logger.info("Simulação criada com sucesso. ID: {}", salva.getIdSimulacao());
        return salva;
    }

    @Transactional
    public Simulacao atualizar(Integer id, Simulacao dadosNovos) {
        logger.info("Atualizando simulação ID: {}", id);

        Simulacao simulacao = simulacaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Simulação não encontrada com ID: " + id));

        simulacao.setNomeCenario(dadosNovos.getNomeCenario());
        simulacao.setPeriodoReferencia(dadosNovos.getPeriodoReferencia());

        Simulacao atualizada = simulacaoRepository.save(simulacao);
        logger.info("Simulação atualizada com sucesso. ID: {}", atualizada.getIdSimulacao());
        return atualizada;
    }

    @Transactional
    public void deletar(Integer id) {
        logger.info("Deletando simulação ID: {}", id);
        if (!simulacaoRepository.existsById(id)) {
            throw new IllegalArgumentException("Simulação não encontrada com ID: " + id);
        }
        simulacaoRepository.deleteById(id);
        logger.info("Simulação deletada com sucesso. ID: {}", id);
    }
}