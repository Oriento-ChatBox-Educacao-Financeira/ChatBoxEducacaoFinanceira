package com.oriento.api.repositories;

import com.oriento.api.model.PlanoConta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanoContaRepository extends JpaRepository<PlanoConta, Integer> {

    @Query("SELECT i FROM PlanoConta i WHERE i.idEmpresa.id = :idEmpresa")
    List<PlanoConta> buscarPorEmpresa(@Param("idEmpresa") Integer idEmpresa);

    @Query("SELECT p FROM PlanoConta p WHERE p.idConta = :idConta AND p.idEmpresa.id = :idEmpresa")
    Optional<PlanoConta> findByIdContaAndEmpresaId(@Param("idConta") Integer idConta, @Param("idEmpresa") Integer idEmpresa);
}