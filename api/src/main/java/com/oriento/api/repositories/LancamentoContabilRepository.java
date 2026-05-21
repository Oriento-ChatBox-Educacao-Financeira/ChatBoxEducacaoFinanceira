package com.oriento.api.repositories;

import com.oriento.api.model.LancamentoContabil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LancamentoContabilRepository extends JpaRepository<LancamentoContabil, Integer> {

    List<LancamentoContabil> findByIdEmpresaId(Integer idEmpresa);
}