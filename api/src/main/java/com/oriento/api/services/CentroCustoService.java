package com.oriento.api.services;

import com.oriento.api.model.CentroCusto;
import com.oriento.api.model.Empresa;
import com.oriento.api.repositories.CentroCustoRepository;
import com.oriento.api.repositories.EmpresaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CentroCustoService {

    private static final Logger logger = LoggerFactory.getLogger(CentroCustoService.class);

    private final CentroCustoRepository centroCustoRepository;
    private final EmpresaRepository empresaRepository;

    public CentroCustoService(CentroCustoRepository centroCustoRepository,
                              EmpresaRepository empresaRepository) {
        this.centroCustoRepository = centroCustoRepository;
        this.empresaRepository = empresaRepository;
    }

    @Transactional(readOnly = true)
    public List<CentroCusto> listarTodos() {
        logger.debug("Listando todos os centros de custo");
        return centroCustoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<CentroCusto> listarPorEmpresa(Integer idEmpresa) {
        logger.debug("Listando centros de custo da empresa ID: {}", idEmpresa);
        return centroCustoRepository.buscarPorEmpresaId(idEmpresa);
    }

    @Transactional(readOnly = true)
    public CentroCusto buscarPorId(Integer id) {
        logger.debug("Buscando centro de custo ID: {}", id);
        return centroCustoRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Centro de custo não encontrado. ID: {}", id);
                    return new IllegalArgumentException("Centro de custo não encontrado com ID: " + id);
                });
    }

    @Transactional
    public CentroCusto criar(CentroCusto centroCusto, Integer idEmpresa) {
        logger.info("Criando centro de custo para empresa ID: {}", idEmpresa);

        Empresa empresa = empresaRepository.findById(idEmpresa)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada com ID: " + idEmpresa));

        centroCusto.setId_empresa(empresa);

        CentroCusto salvo = centroCustoRepository.save(centroCusto);
        logger.info("Centro de custo criado com sucesso. ID: {}", salvo.getIdcentrocusto());
        return salvo;
    }

    @Transactional
    public CentroCusto atualizar(Integer id, CentroCusto dadosNovos) {
        logger.info("Atualizando centro de custo ID: {}", id);

        CentroCusto centroCusto = centroCustoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Centro de custo não encontrado com ID: " + id));

        centroCusto.setCodigo(dadosNovos.getCodigo());
        centroCusto.setNome(dadosNovos.getNome());
        centroCusto.setTipo(dadosNovos.getTipo());
        centroCusto.setAtivo(dadosNovos.getAtivo());

        CentroCusto atualizado = centroCustoRepository.save(centroCusto);
        logger.info("Centro de custo atualizado com sucesso. ID: {}", atualizado.getIdcentrocusto());
        return atualizado;
    }

    @Transactional
    public void deletar(Integer id) {
        logger.info("Deletando centro de custo ID: {}", id);
        if (!centroCustoRepository.existsById(id)) {
            throw new IllegalArgumentException("Centro de custo não encontrado com ID: " + id);
        }
        centroCustoRepository.deleteById(id);
        logger.info("Centro de custo deletado com sucesso. ID: {}", id);
    }
}