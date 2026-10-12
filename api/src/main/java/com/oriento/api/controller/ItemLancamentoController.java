package com.oriento.api.controller;

import com.oriento.api.model.ItemLancamento;
import com.oriento.api.services.ItemLancamentoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/itens-lancamento")
public class ItemLancamentoController {

    private static final Logger logger = LoggerFactory.getLogger(ItemLancamentoController.class);

    private final ItemLancamentoService itemLancamentoService;

    public ItemLancamentoController(ItemLancamentoService itemLancamentoService) {
        this.itemLancamentoService = itemLancamentoService;
    }

    /** GET /api/itens-lancamento */
    @GetMapping
    public ResponseEntity<List<ItemLancamento>> listarTodos() {
        logger.debug("GET /api/itens-lancamento");
        return ResponseEntity.ok(itemLancamentoService.listarTodos());
    }

    /** GET /api/itens-lancamento/lancamento/{idLancamento} */
    @GetMapping("/lancamento/{idLancamento}")
    public ResponseEntity<List<ItemLancamento>> listarPorLancamento(@PathVariable Integer idLancamento) {
        logger.debug("GET /api/itens-lancamento/lancamento/{}", idLancamento);
        return ResponseEntity.ok(itemLancamentoService.listarPorLancamento(idLancamento));
    }

    /** GET /api/itens-lancamento/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<ItemLancamento> buscarPorId(@PathVariable Integer id) {
        logger.debug("GET /api/itens-lancamento/{}", id);
        return ResponseEntity.ok(itemLancamentoService.buscarPorId(id));
    }

    /**
     * POST /api/itens-lancamento/lancamento/{idLancamento}
     * Query params obrigatório: idConta
     * Query param opcional: idCentroCusto
     */
    @PostMapping("/lancamento/{idLancamento}")
    public ResponseEntity<ItemLancamento> criar(@RequestBody ItemLancamento item,
                                                 @PathVariable Integer idLancamento,
                                                 @RequestParam Integer idConta,
                                                 @RequestParam(required = false) Integer idCentroCusto) {
        logger.info("POST /api/itens-lancamento/lancamento/{}", idLancamento);
        ItemLancamento criado = itemLancamentoService.criar(item, idLancamento, idConta, idCentroCusto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    /**
     * PUT /api/itens-lancamento/{id}
     * Query params: idConta (opcional), idCentroCusto (opcional)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ItemLancamento> atualizar(@PathVariable Integer id,
                                                     @RequestBody ItemLancamento dadosNovos,
                                                     @RequestParam(required = false) Integer idConta,
                                                     @RequestParam(required = false) Integer idCentroCusto) {
        logger.info("PUT /api/itens-lancamento/{}", id);
        return ResponseEntity.ok(itemLancamentoService.atualizar(id, dadosNovos, idConta, idCentroCusto));
    }

    /** DELETE /api/itens-lancamento/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        logger.info("DELETE /api/itens-lancamento/{}", id);
        itemLancamentoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
