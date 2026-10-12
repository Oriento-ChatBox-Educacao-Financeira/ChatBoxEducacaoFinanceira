package com.oriento.api.services;

import com.oriento.api.model.enuns.Nivelmaturidadefinanceira;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para criação e atualização de usuários.
 *
 * Usado nos endpoints:
 * - POST /api/usuarios        → criar()
 * - PUT  /api/usuarios/{id}   → atualizar()
 *
 * Observações:
 * - senha é opcional na atualização (se em branco, a senha atual é mantida)
 * - cnpj, razaoSocial e nomeFantasia são opcionais
 */
public record UsuarioRequest(

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String nome,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        @Size(max = 150, message = "Email deve ter no máximo 150 caracteres")
        String email,

        // Opcional na atualização — service só atualiza se não for nulo/vazio
        String senha,

        // Campos opcionais da empresa
        String cnpj,
        String razaoSocial,
        String nomeFantasia,

        // Nível de maturidade — se null, mantém o valor atual (ou usa o default BASICO no criar)
        Nivelmaturidadefinanceira nivelMaturidadeFinanceira

) {}