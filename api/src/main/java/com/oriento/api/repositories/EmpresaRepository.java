package com.oriento.api.repositories;

import com.oriento.api.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Integer> {

    boolean existsByCnpj(Long cnpj);
}