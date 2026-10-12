package com.oriento.api.repositories;

import com.oriento.api.model.ConteudoEducacional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConteudoEducacionalRepository extends JpaRepository<ConteudoEducacional, Integer> {

    List<ConteudoEducacional> findByCategoria(String categoria);
}