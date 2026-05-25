package com.oriento.api.repositories;

import com.oriento.api.model.Orcamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrcamentoRepository extends JpaRepository<Orcamento, Integer> {

    List<Orcamento> findByEmpresaIdId(Integer idEmpresa);
}