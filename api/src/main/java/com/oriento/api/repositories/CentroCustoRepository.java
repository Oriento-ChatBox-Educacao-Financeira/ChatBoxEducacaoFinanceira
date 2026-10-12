package com.oriento.api.repositories;

import com.oriento.api.model.CentroCusto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;


@Repository
public interface CentroCustoRepository extends JpaRepository<CentroCusto, Integer> {

    @Query("SELECT c FROM CentroCusto c WHERE c.id_empresa.id = :idEmpresa")
    List<CentroCusto> buscarPorEmpresaId(@Param("idEmpresa") Integer idEmpresa);
}