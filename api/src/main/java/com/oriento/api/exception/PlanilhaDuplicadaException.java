package com.oriento.api.exception;

/**
 * Lan\u00e7ada quando um upload de planilha cuja hash SHA-256 j\u00e1 existe na
 * tabela {@code linha_demonstrativo} \u00e9 detectado. O
 * {@code GlobalExceptionHandler} mapeia para HTTP 409.
 */
public class PlanilhaDuplicadaException extends RuntimeException {

    private final String arquivoOriginal;

    public PlanilhaDuplicadaException(String mensagem, String arquivoOriginal) {
        super(mensagem);
        this.arquivoOriginal = arquivoOriginal;
    }

    public String getArquivoOriginal() {
        return arquivoOriginal;
    }
}
