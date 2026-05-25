package com.oriento.api.repositories;

import com.oriento.api.model.ParametroSimulacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParametroSimulacaoRepository extends JpaRepository<ParametroSimulacao, Integer> {

    List<ParametroSimulacao> findBySimulacaoIdSimulacao(Integer idSimulacao);
}