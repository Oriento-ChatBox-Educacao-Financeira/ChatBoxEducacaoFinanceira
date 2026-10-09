package com.oriento.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarSenhaDTO(
        @NotBlank(message = "A nova senha \u00e9 obrigat\u00f3ria.")
        @Size(min = 6, message = "A nova senha deve ter pelo menos 6 caracteres.")
        String novaSenha
) {}
