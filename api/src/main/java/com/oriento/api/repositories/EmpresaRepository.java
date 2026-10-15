package com.oriento.api.repositories;

import com.oriento.api.model.Empresa;
import com.oriento.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Integer> {

    boolean existsByCnpj(Long cnpj);

    Optional<Empresa> findByUsuario(Usuario usuario);

    /** Busca empresa associada ao ID do usuário autenticado */
    Optional<Empresa> findByUsuarioIdUsuario(UUID idUsuario);

    /** Busca empresa por ID garantindo que pertence ao usuário autenticado */
    Optional<Empresa> findByIdAndUsuarioIdUsuario(Integer id, UUID idUsuario);
}