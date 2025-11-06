package com.oriento.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CriarUsuarioDTO(
        @NotBlank(message = "O nome é obrigatório.")
        String nome,
        @NotBlank(message = "O CNPJ é obrigatório.")
        @Min(14)
        String cnpj,
        @NotBlank(message = "O email é obrigatório.")
        @Email
        String email,
        String razaoSocial,
        String nomeFantasia,
        @NotBlank(message = "A senha é obrigatória.")
        @Min(8)
        String senha
) {}
