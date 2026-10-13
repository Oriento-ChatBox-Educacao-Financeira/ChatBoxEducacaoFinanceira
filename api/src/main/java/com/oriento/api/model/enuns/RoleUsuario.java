package com.oriento.api.model.enuns;

/**
 * Define os papéis (roles) de acesso ao sistema.
 *
 * Os valores seguem a convenção do Spring Security com prefixo ROLE_,
 * o que permite uso direto com @PreAuthorize("hasRole('ADMIN')") e similares.
 *
 * ROLE_USER  - usuário comum, acesso às próprias funcionalidades
 * ROLE_ADMIN - administrador, acesso total ao sistema
 */
public enum RoleUsuario {
    ROLE_USER,
    ROLE_ADMIN
}
