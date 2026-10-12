package com.oriento.api.repositories;

import com.oriento.api.model.ErroFinanceiroIdentificado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ErroFinanceiroIdentificadoRepository extends JpaRepository<ErroFinanceiroIdentificado, Integer> {

    List<ErroFinanceiroIdentificado> findByEmpresaId(Integer idEmpresa);
}