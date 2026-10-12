package com.oriento.api.security;

import java.util.UUID;

/**
 * Representa o principal autenticado extraído das claims do JWT.
 *
 * É populado pelo JwtAuthenticationConverter customizado e fica disponível
 * em qualquer controller via a anotação @AuthenticationPrincipal:
 *
 * Exemplo de uso:
 *   public ResponseEntity<?> meuEndpoint(@AuthenticationPrincipal UsuarioAutenticado usuario) {
 *       Integer empresaId = usuario.empresaId();
 *       UUID userId = usuario.userId();
 *   }
 */
public record UsuarioAutenticado(
        UUID userId,
        Integer empresaId,
        String email
) {}
