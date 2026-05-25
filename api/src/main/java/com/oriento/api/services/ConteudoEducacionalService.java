package com.oriento.api.services;

import com.oriento.api.model.ConteudoEducacional;
import com.oriento.api.repositories.ConteudoEducacionalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConteudoEducacionalService {

    private static final Logger logger = LoggerFactory.getLogger(ConteudoEducacionalService.class);

    private final ConteudoEducacionalRepository conteudoRepository;

    public ConteudoEducacionalService(ConteudoEducacionalRepository conteudoRepository) {
        this.conteudoRepository = conteudoRepository;
    }

    @Transactional(readOnly = true)
    public List<ConteudoEducacional> listarTodos() {
        logger.debug("Listando todos os conteúdos educacionais");
        return conteudoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ConteudoEducacional> listarPorCategoria(String categoria) {
        logger.debug("Listando conteúdos da categoria: {}", categoria);
        return conteudoRepository.findByCategoria(categoria);
    }

    @Transactional(readOnly = true)
    public ConteudoEducacional buscarPorId(Integer id) {
        logger.debug("Buscando conteúdo educacional ID: {}", id);
        return conteudoRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Conteúdo educacional não encontrado. ID: {}", id);
                    return new IllegalArgumentException("Conteúdo educacional não encontrado com ID: " + id);
                });
    }

    @Transactional
    public ConteudoEducacional criar(ConteudoEducacional conteudo) {
        logger.info("Criando conteúdo educacional: {}", conteudo.getTitulo());
        ConteudoEducacional salvo = conteudoRepository.save(conteudo);
        logger.info("Conteúdo educacional criado com sucesso. ID: {}", salvo.getIdConteudo());
        return salvo;
    }

    @Transactional
    public ConteudoEducacional atualizar(Integer id, ConteudoEducacional dadosNovos) {
        logger.info("Atualizando conteúdo educacional ID: {}", id);

        ConteudoEducacional conteudo = conteudoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conteúdo educacional não encontrado com ID: " + id));

        conteudo.setTitulo(dadosNovos.getTitulo());
        conteudo.setUrlLink(dadosNovos.getUrlLink());
        conteudo.setCategoria(dadosNovos.getCategoria());

        ConteudoEducacional atualizado = conteudoRepository.save(conteudo);
        logger.info("Conteúdo educacional atualizado com sucesso. ID: {}", atualizado.getIdConteudo());
        return atualizado;
    }

    @Transactional
    public void deletar(Integer id) {
        logger.info("Deletando conteúdo educacional ID: {}", id);
        if (!conteudoRepository.existsById(id)) {
            throw new IllegalArgumentException("Conteúdo educacional não encontrado com ID: " + id);
        }
        conteudoRepository.deleteById(id);
        logger.info("Conteúdo educacional deletado com sucesso. ID: {}", id);
    }
}