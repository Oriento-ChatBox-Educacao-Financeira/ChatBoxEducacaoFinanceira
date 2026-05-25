package com.oriento.api.services;

import com.oriento.api.model.CentroCusto;
import com.oriento.api.model.ItemLancamento;
import com.oriento.api.model.LancamentoContabil;
import com.oriento.api.model.PlanoConta;
import com.oriento.api.repositories.CentroCustoRepository;
import com.oriento.api.repositories.ItemLancamentoRepository;
import com.oriento.api.repositories.LancamentoContabilRepository;
import com.oriento.api.repositories.PlanoContaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ItemLancamentoService {

    private static final Logger logger = LoggerFactory.getLogger(ItemLancamentoService.class);

    private final ItemLancamentoRepository itemLancamentoRepository;
    private final LancamentoContabilRepository lancamentoRepository;
    private final PlanoContaRepository planoContaRepository;
    private final CentroCustoRepository centroCustoRepository;

    public ItemLancamentoService(ItemLancamentoRepository itemLancamentoRepository,
                                 LancamentoContabilRepository lancamentoRepository,
                                 PlanoContaRepository planoContaRepository,
                                 CentroCustoRepository centroCustoRepository) {
        this.itemLancamentoRepository = itemLancamentoRepository;
        this.lancamentoRepository = lancamentoRepository;
        this.planoContaRepository = planoContaRepository;
        this.centroCustoRepository = centroCustoRepository;
    }

    @Transactional(readOnly = true)
    public List<ItemLancamento> listarTodos() {
        logger.debug("Listando todos os itens de lançamento");
        return itemLancamentoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ItemLancamento> listarPorLancamento(Integer idLancamento) {
        logger.debug("Listando itens do lançamento ID: {}", idLancamento);
        return itemLancamentoRepository.buscarPorLancamentoId(idLancamento);
    }

    @Transactional(readOnly = true)
    public ItemLancamento buscarPorId(Integer id) {
        logger.debug("Buscando item de lançamento ID: {}", id);
        return itemLancamentoRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Item de lançamento não encontrado. ID: {}", id);
                    return new IllegalArgumentException("Item de lançamento não encontrado com ID: " + id);
                });
    }

    @Transactional
    public ItemLancamento criar(ItemLancamento item, Integer idLancamento,
                                Integer idConta, Integer idCentroCusto) {
        logger.info("Criando item de lançamento para lançamento ID: {}", idLancamento);

        // Validação da constraint: apenas débito OU crédito pode ser > 0
        validarDebitoCredito(item.getValorDebito(), item.getValorCredito());

        LancamentoContabil lancamento = lancamentoRepository.findById(idLancamento)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado com ID: " + idLancamento));

        PlanoConta conta = planoContaRepository.findById(idConta)
                .orElseThrow(() -> new IllegalArgumentException("Plano de conta não encontrado com ID: " + idConta));

        item.setId_lancamento(lancamento);
        item.setId_conta(conta);

        // Centro de custo é opcional
        if (idCentroCusto != null) {
            CentroCusto centroCusto = centroCustoRepository.findById(idCentroCusto)
                    .orElseThrow(() -> new IllegalArgumentException("Centro de custo não encontrado com ID: " + idCentroCusto));
            item.setId_centro_custo(centroCusto);
        }

        ItemLancamento salvo = itemLancamentoRepository.save(item);
        logger.info("Item de lançamento criado com sucesso. ID: {}", salvo.getId());
        return salvo;
    }

    @Transactional
    public ItemLancamento atualizar(Integer id, ItemLancamento dadosNovos,
                                    Integer idConta, Integer idCentroCusto) {
        logger.info("Atualizando item de lançamento ID: {}", id);

        validarDebitoCredito(dadosNovos.getValorDebito(), dadosNovos.getValorCredito());

        ItemLancamento item = itemLancamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item de lançamento não encontrado com ID: " + id));

        item.setValorDebito(dadosNovos.getValorDebito());
        item.setValorCredito(dadosNovos.getValorCredito());

        if (idConta != null) {
            PlanoConta conta = planoContaRepository.findById(idConta)
                    .orElseThrow(() -> new IllegalArgumentException("Plano de conta não encontrado com ID: " + idConta));
            item.setId_conta(conta);
        }

        if (idCentroCusto != null) {
            CentroCusto centroCusto = centroCustoRepository.findById(idCentroCusto)
                    .orElseThrow(() -> new IllegalArgumentException("Centro de custo não encontrado com ID: " + idCentroCusto));
            item.setId_centro_custo(centroCusto);
        } else {
            item.setId_centro_custo(null);
        }

        ItemLancamento atualizado = itemLancamentoRepository.save(item);
        logger.info("Item de lançamento atualizado com sucesso. ID: {}", atualizado.getId());
        return atualizado;
    }

    @Transactional
    public void deletar(Integer id) {
        logger.info("Deletando item de lançamento ID: {}", id);
        if (!itemLancamentoRepository.existsById(id)) {
            throw new IllegalArgumentException("Item de lançamento não encontrado com ID: " + id);
        }
        itemLancamentoRepository.deleteById(id);
        logger.info("Item de lançamento deletado com sucesso. ID: {}", id);
    }

    /**
     * Replica a CONSTRAINT chk_valor do banco no nível de aplicação:
     * (valor_debito > 0 AND valor_credito = 0) OR (valor_credito > 0 AND valor_debito = 0)
     */
    private void validarDebitoCredito(BigDecimal debito, BigDecimal credito) {
        boolean debitoPositivo = debito != null && debito.compareTo(BigDecimal.ZERO) > 0;
        boolean creditoPositivo = credito != null && credito.compareTo(BigDecimal.ZERO) > 0;

        if (debitoPositivo == creditoPositivo) {
            throw new IllegalArgumentException(
                    "O item deve ter apenas débito OU apenas crédito com valor maior que zero.");
        }
    }
}