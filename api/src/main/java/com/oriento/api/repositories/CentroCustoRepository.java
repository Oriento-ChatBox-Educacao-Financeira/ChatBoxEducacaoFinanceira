package com.oriento.api.repositories;

import com.oriento.api.model.CentroCusto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CentroCustoRepository extends JpaRepository<CentroCusto, Integer> {

    List<CentroCusto> findByIdEmpresaId(Integer idEmpresa);
}