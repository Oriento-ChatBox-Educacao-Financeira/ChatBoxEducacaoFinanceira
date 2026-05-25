package com.oriento.api.repositories;

import com.oriento.api.model.LancamentoContabil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LancamentoContabilRepository extends JpaRepository<LancamentoContabil, Integer> {

    @Query("SELECT c FROM LancamentoContabil c WHERE c.idEmpresa.id = :idEmpresa")
    List<LancamentoContabil> buscarPorEmpresaId(@Param("idEmpresa") Integer idEmpresa);
}