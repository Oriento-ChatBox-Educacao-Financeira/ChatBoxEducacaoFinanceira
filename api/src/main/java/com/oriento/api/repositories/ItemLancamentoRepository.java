package com.oriento.api.repositories;

import com.oriento.api.model.ItemLancamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemLancamentoRepository extends JpaRepository<ItemLancamento, Integer> {

    @Query("SELECT i FROM ItemLancamento i WHERE i.id_lancamento.id = :idLancamento")
    List<ItemLancamento> buscarPorLancamentoId(@Param("idLancamento") Integer idLancamento);
}