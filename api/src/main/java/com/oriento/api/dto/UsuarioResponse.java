package com.oriento.api.dto;

import com.oriento.api.model.Empresa;
import com.oriento.api.model.Usuario;

import java.util.UUID;

/**
 * Dados p\u00fablicos do usu\u00e1rio retornados para o frontend.
 * Inclui o snapshot da empresa associada (CNPJ, nome fantasia, raz\u00e3o social)
 * para que a aplica\u00e7\u00e3o n\u00e3o precise fazer um GET separado para mostrar
 * essas informa\u00e7\u00f5es no perfil/configura\u00e7\u00f5es.
 */
public record UsuarioResponse(
        UUID id,
        String nome,
        String email,
        EmpresaResponse empresa
) {

    public static UsuarioResponse fromEntity(Usuario usuario) {
        Empresa empresa = usuario.getEmpresa();
        EmpresaResponse empresaResponse = empresa == null
                ? null
                : new EmpresaResponse(
                        empresa.getId(),
                        empresa.getCnpj(),
                        empresa.getNomeFantasia(),
                        empresa.getRazaoSocial());
        return new UsuarioResponse(
                usuario.getIdUsuario(),
                usuario.getNome(),
                usuario.getEmail(),
                empresaResponse);
    }

    public record EmpresaResponse(
            Integer id,
            String cnpj,
            String nomeFantasia,
            String razaoSocial
    ) {}
}
