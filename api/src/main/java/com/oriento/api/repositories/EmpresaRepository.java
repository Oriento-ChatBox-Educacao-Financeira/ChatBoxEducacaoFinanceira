package com.oriento.api.repositories;

import com.oriento.api.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Integer> {

    boolean existsByCnpj(String cnpj);

    Optional<Empresa> findByUsuario_IdUsuario(UUID idUsuario);
}
