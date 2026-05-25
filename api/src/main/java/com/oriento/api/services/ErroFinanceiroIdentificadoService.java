package com.oriento.api.services;

import com.oriento.api.model.ConteudoEducacional;
import com.oriento.api.model.Empresa;
import com.oriento.api.model.ErroFinanceiroIdentificado;
import com.oriento.api.repositories.ConteudoEducacionalRepository;
import com.oriento.api.repositories.EmpresaRepository;
import com.oriento.api.repositories.ErroFinanceiroIdentificadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ErroFinanceiroIdentificadoService {

    private static final Logger logger = LoggerFactory.getLogger(ErroFinanceiroIdentificadoService.class);

    private final ErroFinanceiroIdentificadoRepository erroRepository;
    private final EmpresaRepository empresaRepository;
    private final ConteudoEducacionalRepository conteudoRepository;

    public ErroFinanceiroIdentificadoService(ErroFinanceiroIdentificadoRepository erroRepository,
                                             EmpresaRepository empresaRepository,
                                             ConteudoEducacionalRepository conteudoRepository) {
        this.erroRepository = erroRepository;
        this.empresaRepository = empresaRepository;
        this.conteudoRepository = conteudoRepository;
    }

    @Transactional(readOnly = true)
    public List<ErroFinanceiroIdentificado> listarTodos() {
        logger.debug("Listando todos os erros financeiros identificados");
        return erroRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ErroFinanceiroIdentificado> listarPorEmpresa(Integer idEmpresa) {
        logger.debug("Listando erros financeiros da empresa ID: {}", idEmpresa);
        return erroRepository.findByEmpresaId(idEmpresa);
    }

    @Transactional(readOnly = true)
    public ErroFinanceiroIdentificado buscarPorId(Integer id) {
        logger.debug("Buscando erro financeiro ID: {}", id);
        return erroRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Erro financeiro não encontrado. ID: {}", id);
                    return new IllegalArgumentException("Erro financeiro não encontrado com ID: " + id);
                });
    }

    @Transactional
    public ErroFinanceiroIdentificado criar(ErroFinanceiroIdentificado erro,
                                            Integer idEmpresa, Integer idConteudo) {
        logger.info("Criando erro financeiro para empresa ID: {}", idEmpresa);

        Empresa empresa = empresaRepository.findById(idEmpresa)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada com ID: " + idEmpresa));

        erro.setEmpresa(empresa);

        // Conteúdo recomendado é opcional — a IA pode ou não vincular um material
        if (idConteudo != null) {
            ConteudoEducacional conteudo = conteudoRepository.findById(idConteudo)
                    .orElseThrow(() -> new IllegalArgumentException("Conteúdo educacional não encontrado com ID: " + idConteudo));
            erro.setConteudoRecomendado(conteudo);
        }

        ErroFinanceiroIdentificado salvo = erroRepository.save(erro);
        logger.info("Erro financeiro criado com sucesso. ID: {}", salvo.getIdErro());
        return salvo;
    }

    @Transactional
    public ErroFinanceiroIdentificado atualizar(Integer id, ErroFinanceiroIdentificado dadosNovos,
                                                Integer idConteudo) {
        logger.info("Atualizando erro financeiro ID: {}", id);

        ErroFinanceiroIdentificado erro = erroRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Erro financeiro não encontrado com ID: " + id));

        erro.setDescricaoErro(dadosNovos.getDescricaoErro());

        if (idConteudo != null) {
            ConteudoEducacional conteudo = conteudoRepository.findById(idConteudo)
                    .orElseThrow(() -> new IllegalArgumentException("Conteúdo educacional não encontrado com ID: " + idConteudo));
            erro.setConteudoRecomendado(conteudo);
        } else {
            erro.setConteudoRecomendado(null);
        }

        ErroFinanceiroIdentificado atualizado = erroRepository.save(erro);
        logger.info("Erro financeiro atualizado com sucesso. ID: {}", atualizado.getIdErro());
        return atualizado;
    }

    @Transactional
    public void deletar(Integer id) {
        logger.info("Deletando erro financeiro ID: {}", id);
        if (!erroRepository.existsById(id)) {
            throw new IllegalArgumentException("Erro financeiro não encontrado com ID: " + id);
        }
        erroRepository.deleteById(id);
        logger.info("Erro financeiro deletado com sucesso. ID: {}", id);
    }
}