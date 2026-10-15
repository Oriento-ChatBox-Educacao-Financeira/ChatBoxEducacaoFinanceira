package com.oriento.api.services;

import com.oriento.api.model.DemonstrativoOficial;
import com.oriento.api.model.Empresa;
import com.oriento.api.repositories.DemonstrativoOficialRepository;
import com.oriento.api.repositories.EmpresaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DemonstrativoOficialService {

    private static final Logger logger = LoggerFactory.getLogger(DemonstrativoOficialService.class);

    private final DemonstrativoOficialRepository demonstrativoRepository;
    private final EmpresaRepository empresaRepository;

    public DemonstrativoOficialService(DemonstrativoOficialRepository demonstrativoRepository,
                                       EmpresaRepository empresaRepository) {
        this.demonstrativoRepository = demonstrativoRepository;
        this.empresaRepository = empresaRepository;
    }

    /**
     * Lista demonstrativos pertencentes à empresa autenticada (anti-IDOR).
     */
    @Transactional(readOnly = true)
    public List<DemonstrativoOficial> listarPorEmpresa(Integer idEmpresa) {
        logger.debug("Listando demonstrativos da empresa ID: {}", idEmpresa);
        return demonstrativoRepository.findByEmpresaId(idEmpresa);
    }

    /**
     * Busca demonstrativo verificando se pertence à empresa autenticada (anti-IDOR).
     */
    @Transactional(readOnly = true)
    public DemonstrativoOficial buscarPorId(Integer id, Integer idEmpresa) {
        logger.debug("Buscando demonstrativo ID: {} para empresa ID: {}", id, idEmpresa);
        return demonstrativoRepository.findByIdDemonstrativoAndEmpresaId(id, idEmpresa)
                .orElseThrow(() -> {
                    logger.warn("Demonstrativo não encontrado ou não pertence à empresa. ID: {}", id);
                    return new IllegalArgumentException("Demonstrativo não encontrado com ID: " + id);
                });
    }

    @Transactional
    public DemonstrativoOficial criar(DemonstrativoOficial demonstrativo, Integer idEmpresa) {
        logger.info("Criando demonstrativo para empresa ID: {}", idEmpresa);

        // Validação de período
        if (demonstrativo.getDataFim().isBefore(demonstrativo.getDataInicio())) {
            throw new IllegalArgumentException("A data fim não pode ser anterior à data início.");
        }

        Empresa empresa = empresaRepository.findById(idEmpresa)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada com ID: " + idEmpresa));

        demonstrativo.setEmpresa(empresa);

        DemonstrativoOficial salvo = demonstrativoRepository.save(demonstrativo);
        logger.info("Demonstrativo criado com sucesso. ID: {}", salvo.getIdDemonstrativo());
        return salvo;
    }

    @Transactional
    public DemonstrativoOficial atualizar(Integer id, DemonstrativoOficial dadosNovos, Integer idEmpresa) {
        logger.info("Atualizando demonstrativo ID: {} para empresa ID: {}", id, idEmpresa);

        if (dadosNovos.getDataFim().isBefore(dadosNovos.getDataInicio())) {
            throw new IllegalArgumentException("A data fim não pode ser anterior à data início.");
        }

        DemonstrativoOficial demonstrativo = demonstrativoRepository.findByIdDemonstrativoAndEmpresaId(id, idEmpresa)
                .orElseThrow(() -> new IllegalArgumentException("Demonstrativo não encontrado com ID: " + id));

        demonstrativo.setTipo(dadosNovos.getTipo());
        demonstrativo.setDataInicio(dadosNovos.getDataInicio());
        demonstrativo.setDataFim(dadosNovos.getDataFim());
        demonstrativo.setDadosJson(dadosNovos.getDadosJson());

        DemonstrativoOficial atualizado = demonstrativoRepository.save(demonstrativo);
        logger.info("Demonstrativo atualizado com sucesso. ID: {}", atualizado.getIdDemonstrativo());
        return atualizado;
    }

    @Transactional
    public void deletar(Integer id, Integer idEmpresa) {
        logger.info("Deletando demonstrativo ID: {} para empresa ID: {}", id, idEmpresa);
        DemonstrativoOficial demonstrativo = demonstrativoRepository.findByIdDemonstrativoAndEmpresaId(id, idEmpresa)
                .orElseThrow(() -> new IllegalArgumentException("Demonstrativo não encontrado com ID: " + id));
        demonstrativoRepository.delete(demonstrativo);
        logger.info("Demonstrativo deletado com sucesso. ID: {}", id);
    }
}