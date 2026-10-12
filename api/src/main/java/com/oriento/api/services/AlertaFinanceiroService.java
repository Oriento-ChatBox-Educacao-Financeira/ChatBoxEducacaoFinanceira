package com.oriento.api.services;

import com.oriento.api.model.AlertaFinanceiro;
import com.oriento.api.model.Empresa;
import com.oriento.api.repositories.AlertaFinanceiroRepository;
import com.oriento.api.repositories.EmpresaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AlertaFinanceiroService {

    private static final Logger logger = LoggerFactory.getLogger(AlertaFinanceiroService.class);

    private final AlertaFinanceiroRepository alertaRepository;
    private final EmpresaRepository empresaRepository;

    public AlertaFinanceiroService(AlertaFinanceiroRepository alertaRepository,
                                   EmpresaRepository empresaRepository) {
        this.alertaRepository = alertaRepository;
        this.empresaRepository = empresaRepository;
    }

    @Transactional(readOnly = true)
    public List<AlertaFinanceiro> listarTodos() {
        logger.debug("Listando todos os alertas financeiros");
        return alertaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<AlertaFinanceiro> listarPorEmpresa(Integer idEmpresa) {
        logger.debug("Listando alertas da empresa ID: {}", idEmpresa);
        return alertaRepository.findByEmpresaId(idEmpresa);
    }

    @Transactional(readOnly = true)
    public AlertaFinanceiro buscarPorId(Integer id) {
        logger.debug("Buscando alerta financeiro ID: {}", id);
        return alertaRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Alerta financeiro não encontrado. ID: {}", id);
                    return new IllegalArgumentException("Alerta financeiro não encontrado com ID: " + id);
                });
    }

    @Transactional
    public AlertaFinanceiro criar(AlertaFinanceiro alerta, Integer idEmpresa) {
        logger.info("Criando alerta financeiro para empresa ID: {}", idEmpresa);

        Empresa empresa = empresaRepository.findById(idEmpresa)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada com ID: " + idEmpresa));

        alerta.setEmpresa(empresa);

        AlertaFinanceiro salvo = alertaRepository.save(alerta);
        logger.info("Alerta financeiro criado com sucesso. ID: {}", salvo.getIdAlerta());
        return salvo;
    }

    @Transactional
    public AlertaFinanceiro atualizar(Integer id, AlertaFinanceiro dadosNovos) {
        logger.info("Atualizando alerta financeiro ID: {}", id);

        AlertaFinanceiro alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alerta financeiro não encontrado com ID: " + id));

        alerta.setTipoAlerta(dadosNovos.getTipoAlerta());
        alerta.setMensagem(dadosNovos.getMensagem());
        alerta.setSeveridade(dadosNovos.getSeveridade());

        AlertaFinanceiro atualizado = alertaRepository.save(alerta);
        logger.info("Alerta financeiro atualizado com sucesso. ID: {}", atualizado.getIdAlerta());
        return atualizado;
    }

    @Transactional
    public void deletar(Integer id) {
        logger.info("Deletando alerta financeiro ID: {}", id);
        if (!alertaRepository.existsById(id)) {
            throw new IllegalArgumentException("Alerta financeiro não encontrado com ID: " + id);
        }
        alertaRepository.deleteById(id);
        logger.info("Alerta financeiro deletado com sucesso. ID: {}", id);
    }
}