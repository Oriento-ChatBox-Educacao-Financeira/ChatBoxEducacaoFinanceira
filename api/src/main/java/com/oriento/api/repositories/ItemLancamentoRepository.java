package com.oriento.api.repositories;

import com.oriento.api.model.ItemLancamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemLancamentoRepository extends JpaRepository<ItemLancamento, Integer> {

    List<ItemLancamento> findByIdLancamentoId(Integer idLancamento);
}