package com.oriento.api.repositories;

import com.oriento.api.model.Simulacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SimulacaoRepository extends JpaRepository<Simulacao, Integer> {

    List<Simulacao> findByEmpresaId(Integer idEmpresa);
}