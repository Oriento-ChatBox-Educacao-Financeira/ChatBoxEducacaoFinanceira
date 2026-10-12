package com.oriento.api.repositories;

import com.oriento.api.model.ResultadoSimulacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResultadoSimulacaoRepository extends JpaRepository<ResultadoSimulacao, Integer> {

    Optional<ResultadoSimulacao> findBySimulacaoIdSimulacao(Integer idSimulacao);
}