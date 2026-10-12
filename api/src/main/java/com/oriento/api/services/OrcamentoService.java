package com.oriento.api.services;

import com.oriento.api.model.CentroCusto;
import com.oriento.api.model.Empresa;
import com.oriento.api.model.Orcamento;
import com.oriento.api.model.PlanoConta;
import com.oriento.api.repositories.CentroCustoRepository;
import com.oriento.api.repositories.EmpresaRepository;
import com.oriento.api.repositories.OrcamentoRepository;
import com.oriento.api.repositories.PlanoContaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrcamentoService {

    private static final Logger logger = LoggerFactory.getLogger(OrcamentoService.class);

    private final OrcamentoRepository orcamentoRepository;
    private final EmpresaRepository empresaRepository;
    private final PlanoContaRepository planoContaRepository;
    private final CentroCustoRepository centroCustoRepository;

    public OrcamentoService(OrcamentoRepository orcamentoRepository,
                            EmpresaRepository empresaRepository,
                            PlanoContaRepository planoContaRepository,
                            CentroCustoRepository centroCustoRepository) {
        this.orcamentoRepository = orcamentoRepository;
        this.empresaRepository = empresaRepository;
        this.planoContaRepository = planoContaRepository;
        this.centroCustoRepository = centroCustoRepository;
    }

    @Transactional(readOnly = true)
    public List<Orcamento> listarTodos() {
        logger.debug("Listando todos os orçamentos");
        return orcamentoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Orcamento> listarPorEmpresa(Integer idEmpresa) {
        logger.debug("Listando orçamentos da empresa ID: {}", idEmpresa);
        return orcamentoRepository.findByEmpresaIdId(idEmpresa);
    }

    @Transactional(readOnly = true)
    public Orcamento buscarPorId(Integer id) {
        logger.debug("Buscando orçamento ID: {}", id);
        return orcamentoRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Orçamento não encontrado. ID: {}", id);
                    return new IllegalArgumentException("Orçamento não encontrado com ID: " + id);
                });
    }

    @Transactional
    public Orcamento criar(Orcamento orcamento, Integer idEmpresa,
                           Integer idConta, Integer idCentroCusto) {
        logger.info("Criando orçamento para empresa ID: {}", idEmpresa);

        Empresa empresa = empresaRepository.findById(idEmpresa)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada com ID: " + idEmpresa));

        PlanoConta conta = planoContaRepository.findById(idConta)
                .orElseThrow(() -> new IllegalArgumentException("Plano de conta não encontrado com ID: " + idConta));

        orcamento.setEmpresaId(empresa);
        orcamento.setContaId(conta);

        // Centro de custo é opcional no modelo lógico
        if (idCentroCusto != null) {
            CentroCusto centroCusto = centroCustoRepository.findById(idCentroCusto)
                    .orElseThrow(() -> new IllegalArgumentException("Centro de custo não encontrado com ID: " + idCentroCusto));
            orcamento.setCentroCustoId(centroCusto);
        }

        Orcamento salvo = orcamentoRepository.save(orcamento);
        logger.info("Orçamento criado com sucesso. ID: {}", salvo.getId());
        return salvo;
    }

    @Transactional
    public Orcamento atualizar(Integer id, Orcamento dadosNovos) {
        logger.info("Atualizando orçamento ID: {}", id);

        Orcamento orcamento = orcamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado com ID: " + id));

        orcamento.setAno(dadosNovos.getAno());
        orcamento.setMes(dadosNovos.getMes());
        orcamento.setValorPlanejado(dadosNovos.getValorPlanejado());
        orcamento.setAprovado(dadosNovos.getAprovado());

        Orcamento atualizado = orcamentoRepository.save(orcamento);
        logger.info("Orçamento atualizado com sucesso. ID: {}", atualizado.getId());
        return atualizado;
    }

    @Transactional
    public void deletar(Integer id) {
        logger.info("Deletando orçamento ID: {}", id);
        if (!orcamentoRepository.existsById(id)) {
            throw new IllegalArgumentException("Orçamento não encontrado com ID: " + id);
        }
        orcamentoRepository.deleteById(id);
        logger.info("Orçamento deletado com sucesso. ID: {}", id);
    }
}