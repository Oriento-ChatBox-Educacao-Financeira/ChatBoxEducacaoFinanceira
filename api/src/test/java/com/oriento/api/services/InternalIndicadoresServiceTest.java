package com.oriento.api.services;

import com.oriento.api.dto.EmpresaIndicadoresResponse;
import com.oriento.api.model.*;
import com.oriento.api.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class InternalIndicadoresServiceTest {
    final EmpresaRepository companies = mock(EmpresaRepository.class);
    final IndicadorFinanceiroRepository indicators = mock(IndicadorFinanceiroRepository.class);
    final InternalIndicadoresService service = new InternalIndicadoresService(companies, indicators);

    @Test
    void preservesDecimalPrecisionNullAndChronologicalOrder() {
        Usuario user = owner();
        Empresa company = new Empresa(7);
        company.setUsuario(user);
        when(companies.findById(7)).thenReturn(Optional.of(company));
        IndicadorFinanceiro recent = indicator(2, "2026-09-01");
        recent.setReceitaTotal(new BigDecimal("900719925474099.91"));
        IndicadorFinanceiro older = indicator(1, "2026-08-01");
        when(indicators.findTop12ByEmpresaIdOrderByAnoMesDesc(7)).thenReturn(List.of(recent, older));
        EmpresaIndicadoresResponse result = service.get(7, user);
        assertThat(result.indicadores()).extracting(EmpresaIndicadoresResponse.Indicador::idIndicador).containsExactly(1, 2);
        assertThat(result.indicadores().get(1).receitaTotal()).isEqualTo("900719925474099.91");
        assertThat(result.indicadores().get(1).fluxoCaixa()).isNull();
    }

    @Test
    void foreignCompanyIsForbiddenBeforeReadingIndicators() {
        Empresa company = new Empresa(7);
        company.setUsuario(owner());
        when(companies.findById(7)).thenReturn(Optional.of(company));
        assertThatThrownBy(() -> service.get(7, owner())).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(indicators);
    }

    @Test
    void missingCompanyIsNotFound() {
        when(companies.findById(7)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(7, owner())).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void existingCompanyCanHaveNoIndicators() {
        Usuario user = owner();
        Empresa company = new Empresa(7);
        company.setUsuario(user);
        when(companies.findById(7)).thenReturn(Optional.of(company));
        when(indicators.findTop12ByEmpresaIdOrderByAnoMesDesc(7)).thenReturn(List.of());
        assertThat(service.get(7, user).indicadores()).isEmpty();
    }

    private Usuario owner() {
        Usuario user = new Usuario();
        user.setIdUsuario(UUID.randomUUID());
        return user;
    }

    private IndicadorFinanceiro indicator(int id, String date) {
        IndicadorFinanceiro indicator = new IndicadorFinanceiro();
        indicator.setIdIndicador(id);
        indicator.setAnoMes(LocalDate.parse(date));
        return indicator;
    }
}
