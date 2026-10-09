package com.oriento.api.repositories;

import com.oriento.api.model.LinhaDemonstrativo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LinhaDemonstrativoRepository extends JpaRepository<LinhaDemonstrativo, Long> {

    boolean existsByEmpresa_IdAndHashArquivo(Integer idEmpresa, String hashArquivo);

    boolean existsByEmpresa_IdAndArquivoOrigem(Integer idEmpresa, String arquivoOrigem);

    @Query("select distinct l.tipo from LinhaDemonstrativo l where l.empresa.id = :idEmpresa")
    List<String> tiposPresentes(@Param("idEmpresa") Integer idEmpresa);

    List<LinhaDemonstrativo> findByEmpresa_IdOrderByPeriodoAsc(Integer idEmpresa);

    @Modifying
    @Query("delete from LinhaDemonstrativo l where l.empresa.id = :idEmpresa and l.arquivoOrigem = :arquivo")
    int deleteByEmpresaAndArquivo(@Param("idEmpresa") Integer idEmpresa,
                                  @Param("arquivo") String arquivo);

    /**
     * Resumo agrupado por arquivo origem, retornando contagem e \u00faltima data
     * de upload. Usado pela tela de Configura\u00e7\u00f5es.
     */
    @Query("""
            select l.arquivoOrigem as arquivo,
                   max(l.hashArquivo) as hash,
                   count(l) as linhas,
                   max(l.dataUpload) as dataUpload
              from LinhaDemonstrativo l
             where l.empresa.id = :idEmpresa
             group by l.arquivoOrigem
             order by max(l.dataUpload) desc
            """)
    List<PlanilhaResumoProjection> resumirPorArquivo(@Param("idEmpresa") Integer idEmpresa);

    interface PlanilhaResumoProjection {
        String getArquivo();
        String getHash();
        Long getLinhas();
        java.time.OffsetDateTime getDataUpload();
    }
}
