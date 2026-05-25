package com.oriento.api.services;

import com.oriento.api.model.Mensagens;

import com.oriento.api.repositories.MensagensRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class MensagensService {

    @Autowired
    private MensagensRepository repository;


    public Mensagens salvarMensagens(Mensagens Mensagens) {
        return repository.save(Mensagens);
    }


    public List<Mensagens> buscarHistoricoDaConversa(UUID conversaId) {
        return repository.findByConversaIdOrderByOrdemAsc(conversaId);
    }


    public Mensagens atualizarFeedback(String idMensagens, Boolean util, String comentario) {

        Mensagens Mensagens = repository.findById(idMensagens)
                .orElseThrow(() -> new RuntimeException("Mensagens não encontrada"));


        Mensagens.FeedbackUsuario feedback = new Mensagens.FeedbackUsuario();
        feedback.util = util;
        feedback.comentario = comentario;

        Mensagens.setFeedbackUsuario(feedback);


        return repository.save(Mensagens);
    }


    public void deletarMensagens(String idMensagens) {
        repository.deleteById(idMensagens);
    }
}