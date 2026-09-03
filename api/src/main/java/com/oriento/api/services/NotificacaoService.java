package com.oriento.api.services;

import com.oriento.api.model.Notificacao;
import com.oriento.api.repositories.NotificacaoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificacaoService {

    private final NotificacaoRepository repository;

    public NotificacaoService(NotificacaoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Notificacao> listarPorUsuario(UUID idUsuario, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return repository.findByIdUsuarioOrderByDataCriacaoDesc(idUsuario, PageRequest.of(0, safeLimit));
    }

    /**
     * Criada em uma transa\u00e7\u00e3o pr\u00f3pria ({@link Propagation#REQUIRES_NEW}):
     * se falhar (ex.: viola\u00e7\u00e3o de FK, problema tempor\u00e1rio do DB), s\u00f3
     * marca essa transa\u00e7\u00e3o como rollback-only \u2014 a transa\u00e7\u00e3o
     * principal do chamador (ex.: import de planilha) segue comitando.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notificacao criar(UUID idUsuario, String tipo, String titulo, String mensagem,
                             String link, String icone) {
        Notificacao n = new Notificacao();
        n.setIdUsuario(idUsuario);
        n.setTipo(tipo);
        n.setTitulo(titulo);
        n.setMensagem(mensagem);
        n.setLink(link);
        n.setIcone(icone);
        n.setLida(false);
        return repository.save(n);
    }

    @Transactional
    public void marcarComoLida(UUID idUsuario, Long idNotificacao) {
        Notificacao n = repository.findById(idNotificacao)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notifica\u00e7\u00e3o n\u00e3o encontrada"));
        if (!n.getIdUsuario().equals(idUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (!n.isLida()) {
            n.setLida(true);
            n.setDataLeitura(OffsetDateTime.now());
        }
    }

    @Transactional
    public void marcarTodasLidas(UUID idUsuario) {
        repository.marcarTodasLidas(idUsuario, OffsetDateTime.now());
    }
}
