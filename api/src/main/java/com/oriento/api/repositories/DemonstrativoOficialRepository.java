package com.oriento.api.repositories;

import com.oriento.api.model.DemonstrativoOficial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DemonstrativoOficialRepository extends JpaRepository<DemonstrativoOficial, Integer> {

    List<DemonstrativoOficial> findByEmpresaId(Integer idEmpresa);

    Optional<DemonstrativoOficial> findByIdDemonstrativoAndEmpresaId(Integer idDemonstrativo, Integer idEmpresa);
}