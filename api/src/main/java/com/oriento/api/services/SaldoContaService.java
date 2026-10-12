package com.oriento.api.services;

import com.oriento.api.model.Empresa;
import com.oriento.api.model.PlanoConta;
import com.oriento.api.model.SaldoConta;
import com.oriento.api.repositories.EmpresaRepository;
import com.oriento.api.repositories.PlanoContaRepository;
import com.oriento.api.repositories.SaldoContaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SaldoContaService {

    private static final Logger logger = LoggerFactory.getLogger(SaldoContaService.class);

    private final SaldoContaRepository saldoContaRepository;
    private final EmpresaRepository empresaRepository;
    private final PlanoContaRepository planoContaRepository;

    public SaldoContaService(SaldoContaRepository saldoContaRepository,
                             EmpresaRepository empresaRepository,
                             PlanoContaRepository planoContaRepository) {
        this.saldoContaRepository = saldoContaRepository;
        this.empresaRepository = empresaRepository;
        this.planoContaRepository = planoContaRepository;
    }

    @Transactional(readOnly = true)
    public List<SaldoConta> listarTodos() {
        logger.debug("Listando todos os saldos de conta");
        return saldoContaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<SaldoConta> listarPorEmpresa(Integer idEmpresa) {
        logger.debug("Listando saldos da empresa ID: {}", idEmpresa);
        return saldoContaRepository.findByEmpresaId(idEmpresa);
    }

    @Transactional(readOnly = true)
    public SaldoConta buscarPorId(Integer id) {
        logger.debug("Buscando saldo de conta ID: {}", id);
        return saldoContaRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Saldo de conta não encontrado. ID: {}", id);
                    return new IllegalArgumentException("Saldo de conta não encontrado com ID: " + id);
                });
    }

    @Transactional
    public SaldoConta criar(SaldoConta saldoConta, Integer idEmpresa, Integer idConta) {
        logger.info("Criando saldo de conta para empresa ID: {}, conta ID: {}", idEmpresa, idConta);

        Empresa empresa = empresaRepository.findById(idEmpresa)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada com ID: " + idEmpresa));

        PlanoConta conta = planoContaRepository.findById(idConta)
                .orElseThrow(() -> new IllegalArgumentException("Plano de conta não encontrado com ID: " + idConta));

        // Valida unicidade (id_empresa, id_conta, ano, mes) antes de salvar
        if (saldoContaRepository.existsByEmpresaIdAndContaIdAndAnoAndMes(
                idEmpresa, idConta, saldoConta.getAno(), saldoConta.getMes())) {
            throw new IllegalArgumentException(
                    "Já existe saldo cadastrado para esta empresa/conta no período informado.");
        }

        saldoConta.setEmpresa(empresa);
        saldoConta.setConta(conta);

        SaldoConta salvo = saldoContaRepository.save(saldoConta);
        logger.info("Saldo de conta criado com sucesso. ID: {}", salvo.getIdSaldo());
        return salvo;
    }

    @Transactional
    public SaldoConta atualizar(Integer id, SaldoConta dadosNovos) {
        logger.info("Atualizando saldo de conta ID: {}", id);

        SaldoConta saldoConta = saldoContaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Saldo de conta não encontrado com ID: " + id));

        saldoConta.setSaldoInicial(dadosNovos.getSaldoInicial());
        saldoConta.setDebitoPeriodo(dadosNovos.getDebitoPeriodo());
        saldoConta.setCreditoPeriodo(dadosNovos.getCreditoPeriodo());
        saldoConta.setSaldoFinal(dadosNovos.getSaldoFinal());

        SaldoConta atualizado = saldoContaRepository.save(saldoConta);
        logger.info("Saldo de conta atualizado com sucesso. ID: {}", atualizado.getIdSaldo());
        return atualizado;
    }

    @Transactional
    public void deletar(Integer id) {
        logger.info("Deletando saldo de conta ID: {}", id);
        if (!saldoContaRepository.existsById(id)) {
            throw new IllegalArgumentException("Saldo de conta não encontrado com ID: " + id);
        }
        saldoContaRepository.deleteById(id);
        logger.info("Saldo de conta deletado com sucesso. ID: {}", id);
    }
}