package com.oriento.api.services;

import com.oriento.api.model.Empresa;
import com.oriento.api.model.PlanoConta;
import com.oriento.api.repositories.EmpresaRepository;
import com.oriento.api.repositories.PlanoContaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlanoContaService {

    private static final Logger logger = LoggerFactory.getLogger(PlanoContaService.class);

    private final PlanoContaRepository planoContaRepository;
    private final EmpresaRepository empresaRepository;

    public PlanoContaService(PlanoContaRepository planoContaRepository,
                             EmpresaRepository empresaRepository) {
        this.planoContaRepository = planoContaRepository;
        this.empresaRepository = empresaRepository;
    }

    /**
     * Lista planos de conta pertencentes à empresa autenticada (anti-IDOR).
     */
    @Transactional(readOnly = true)
    public List<PlanoConta> listarPorEmpresa(Integer idEmpresa) {
        logger.debug("Listando planos de conta da empresa ID: {}", idEmpresa);
        return planoContaRepository.buscarPorEmpresa(idEmpresa);
    }

    /**
     * Busca plano de conta por ID verificando se pertence à empresa autenticada (anti-IDOR).
     */
    @Transactional(readOnly = true)
    public PlanoConta buscarPorId(Integer id, Integer idEmpresa) {
        logger.debug("Buscando plano de conta ID: {} para empresa ID: {}", id, idEmpresa);
        return planoContaRepository.findByIdContaAndEmpresaId(id, idEmpresa)
                .orElseThrow(() -> {
                    logger.warn("Plano de conta não encontrado ou não pertence à empresa. ID: {}", id);
                    return new IllegalArgumentException("Plano de conta não encontrado com ID: " + id);
                });
    }

    @Transactional
    public PlanoConta criar(PlanoConta planoConta, Integer idEmpresa, Integer idContaPai) {
        logger.info("Criando plano de conta para empresa ID: {}", idEmpresa);

        Empresa empresa = empresaRepository.findById(idEmpresa)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada com ID: " + idEmpresa));

        planoConta.setIdEmpresa(empresa);

        // Víncula conta pai se informada (garantindo que pertence à mesma empresa)
        if (idContaPai != null) {
            PlanoConta contaPai = planoContaRepository.findByIdContaAndEmpresaId(idContaPai, idEmpresa)
                    .orElseThrow(() -> new IllegalArgumentException("Conta pai não encontrada para esta empresa com ID: " + idContaPai));
            planoConta.setContaPai(contaPai);
        }

        PlanoConta salvo = planoContaRepository.save(planoConta);
        logger.info("Plano de conta criado com sucesso. ID: {}", salvo.getIdConta());
        return salvo;
    }

    @Transactional
    public PlanoConta atualizar(Integer id, PlanoConta dadosNovos, Integer idContaPai, Integer idEmpresa) {
        logger.info("Atualizando plano de conta ID: {} para empresa ID: {}", id, idEmpresa);

        PlanoConta planoConta = planoContaRepository.findByIdContaAndEmpresaId(id, idEmpresa)
                .orElseThrow(() -> new IllegalArgumentException("Plano de conta não encontrado com ID: " + id));

        planoConta.setCodigo(dadosNovos.getCodigo());
        planoConta.setNome(dadosNovos.getNome());
        planoConta.setTipo(dadosNovos.getTipo());
        planoConta.setNatureza(dadosNovos.getNatureza());
        planoConta.setNivel(dadosNovos.getNivel());
        planoConta.setPermitelancamento(dadosNovos.getPermitelancamento());

        if (idContaPai != null) {
            PlanoConta contaPai = planoContaRepository.findByIdContaAndEmpresaId(idContaPai, idEmpresa)
                    .orElseThrow(() -> new IllegalArgumentException("Conta pai não encontrada para esta empresa com ID: " + idContaPai));
            planoConta.setContaPai(contaPai);
        } else {
            planoConta.setContaPai(null); // remove hierarquia se não informada
        }

        PlanoConta atualizado = planoContaRepository.save(planoConta);
        logger.info("Plano de conta atualizado com sucesso. ID: {}", atualizado.getIdConta());
        return atualizado;
    }

    @Transactional
    public void deletar(Integer id, Integer idEmpresa) {
        logger.info("Deletando plano de conta ID: {} para empresa ID: {}", id, idEmpresa);
        PlanoConta planoConta = planoContaRepository.findByIdContaAndEmpresaId(id, idEmpresa)
                .orElseThrow(() -> new IllegalArgumentException("Plano de conta não encontrado com ID: " + id));
        planoContaRepository.delete(planoConta);
        logger.info("Plano de conta deletado com sucesso. ID: {}", id);
    }
}