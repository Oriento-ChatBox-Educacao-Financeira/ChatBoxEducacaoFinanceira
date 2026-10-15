package com.oriento.api.services;

import com.oriento.api.dto.EmpresaIndicadoresResponse;
import com.oriento.api.model.Empresa;
import com.oriento.api.model.IndicadorFinanceiro;
import com.oriento.api.model.Usuario;
import com.oriento.api.repositories.EmpresaRepository;
import com.oriento.api.repositories.IndicadorFinanceiroRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InternalIndicadoresService {
    private final EmpresaRepository empresas;
    private final IndicadorFinanceiroRepository indicadores;

    public InternalIndicadoresService(EmpresaRepository empresas, IndicadorFinanceiroRepository indicadores) {
        this.empresas = empresas;
        this.indicadores = indicadores;
    }

    @Transactional(readOnly = true)
    public EmpresaIndicadoresResponse get(Integer empresaId, Usuario usuario) {
        if (empresaId <= 0) {
            throw new IllegalArgumentException("Identificador de empresa inválido");
        }
        Empresa empresa = empresas.findById(empresaId)
                .orElseThrow(() -> new EntityNotFoundException("Empresa não encontrada"));
        if (empresa.getUsuario() == null
                || !usuario.getIdUsuario().equals(empresa.getUsuario().getIdUsuario())) {
            throw new AccessDeniedException("Empresa não pertence ao usuário autenticado");
        }
        List<IndicadorFinanceiro> recent = indicadores.findTop12ByEmpresaIdOrderByAnoMesDesc(empresaId);
        return new EmpresaIndicadoresResponse(empresaId, recent.reversed().stream()
                .map(EmpresaIndicadoresResponse.Indicador::from).toList());
    }
}
