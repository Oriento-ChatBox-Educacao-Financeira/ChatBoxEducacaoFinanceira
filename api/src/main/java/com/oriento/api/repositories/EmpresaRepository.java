package com.oriento.api.repositories;

import com.oriento.api.model.Empresa;
import com.oriento.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Integer> {

    boolean existsByCnpj(Long cnpj);

    Optional<Empresa> findByUsuario(Usuario usuario);
}