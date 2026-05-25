package com.oriento.api.repositories;

import com.oriento.api.model.SaldoConta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaldoContaRepository extends JpaRepository<SaldoConta, Integer> {

    List<SaldoConta> findByEmpresaId(Integer idEmpresa);

    boolean existsByEmpresaIdAndContaIdAndAnoAndMes(Integer idEmpresa, Integer idConta,
                                                    Integer ano, Integer mes);
}