package com.oriento.api.repositories;

import com.oriento.api.model.IndicadorFinanceiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IndicadorFinanceiroRepository extends JpaRepository<IndicadorFinanceiro, Integer> {

    List<IndicadorFinanceiro> findByEmpresaId(Integer idEmpresa);

    Optional<IndicadorFinanceiro> findByIdIndicadorAndEmpresaId(Integer idIndicador, Integer idEmpresa);

    boolean existsByEmpresaIdAndAnoMes(Integer idEmpresa, LocalDate anoMes);
}