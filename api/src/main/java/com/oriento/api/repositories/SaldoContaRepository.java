package com.oriento.api.repositories;

import com.oriento.api.model.SaldoConta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaldoContaRepository extends JpaRepository<SaldoConta, Integer> {

    List<SaldoConta> findByEmpresaId(Integer idEmpresa);  // se funcionar, mantenha

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM SaldoConta s WHERE s.empresa.id = :idEmpresa AND s.conta.id = :idConta AND s.ano = :ano AND s.mes = :mes")
    boolean existsByEmpresaIdAndContaIdAndAnoAndMes(@Param("idEmpresa") Integer idEmpresa,
                                                    @Param("idConta") Integer idConta,
                                                    @Param("ano") Integer ano,
                                                    @Param("mes") Integer mes);
}