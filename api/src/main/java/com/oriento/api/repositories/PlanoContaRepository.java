package com.oriento.api.repositories;

import com.oriento.api.model.PlanoConta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanoContaRepository extends JpaRepository<PlanoConta, Integer> {

    List<PlanoConta> findByIdEmpresaId(Integer idEmpresa);
}