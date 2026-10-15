package com.oriento.api.repositories;

import com.oriento.api.model.AlertaFinanceiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertaFinanceiroRepository extends JpaRepository<AlertaFinanceiro, Integer> {

    List<AlertaFinanceiro> findByEmpresaId(Integer idEmpresa);

    Optional<AlertaFinanceiro> findByIdAlertaAndEmpresaId(Integer idAlerta, Integer idEmpresa);
}