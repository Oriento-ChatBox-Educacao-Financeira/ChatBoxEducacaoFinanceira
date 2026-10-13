package com.oriento.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload do cadastro inicial. O backend cria, em uma transa\u00e7\u00e3o,
 * o {@code Usuario} (Postgres) e a {@code Empresa} correspondente.
 *
 * <p>{@code razaoSocial} \u00e9 opcional. {@code cnpj} chega s\u00f3 com d\u00edgitos
 * (frontend remove m\u00e1scara).
 */
public record CriarUsuarioDTO(
        @NotBlank(message = "O nome \u00e9 obrigat\u00f3rio.")
        @Size(max = 100, message = "O nome deve ter no m\u00e1ximo 100 caracteres.")
        String nome,

        @NotBlank(message = "O e-mail \u00e9 obrigat\u00f3rio.")
        @Email(message = "E-mail inv\u00e1lido.")
        @Size(max = 150, message = "O e-mail deve ter no m\u00e1ximo 150 caracteres.")
        String email,

        @NotBlank(message = "A senha \u00e9 obrigat\u00f3ria.")
        @Size(min = 8, message = "A senha deve conter pelo menos 8 caracteres.")
        String senha,

        @NotBlank(message = "O CNPJ \u00e9 obrigat\u00f3rio.")
        @Size(min = 11, max = 18, message = "CNPJ inv\u00e1lido.")
        String cnpj,

        @NotBlank(message = "O nome fantasia \u00e9 obrigat\u00f3rio.")
        @Size(max = 150, message = "O nome fantasia deve ter no m\u00e1ximo 150 caracteres.")
        String nomeFantasia,

        String razaoSocial
) {}
