package com.oriento.api.model.enuns;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Mapeia o enum Java {@link Regimetributario} para os labels reais do tipo
 * enum Postgres {@code regime_tributario_tipo}, que possui espaços
 * ({@code "Simples Nacional"}, {@code "Lucro Presumido"}, {@code "Lucro Real"})
 * — algo que um identificador Java não consegue representar diretamente.
 *
 * <p>Combinado com {@code stringtype=unspecified} na URL JDBC, o Postgres
 * faz o cast implícito de varchar para o tipo enum customizado.
 */
@Converter(autoApply = false)
public class RegimetributarioConverter implements AttributeConverter<Regimetributario, String> {

    public static final String SIMPLES_NACIONAL = "Simples Nacional";
    public static final String LUCRO_PRESUMIDO = "Lucro Presumido";
    public static final String LUCRO_REAL = "Lucro Real";

    @Override
    public String convertToDatabaseColumn(Regimetributario attribute) {
        if (attribute == null) {
            return null;
        }
        return switch (attribute) {
            case SimplesNacional -> SIMPLES_NACIONAL;
            case LucroPresumido -> LUCRO_PRESUMIDO;
            case LucroReal -> LUCRO_REAL;
        };
    }

    @Override
    public Regimetributario convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return switch (dbData) {
            case SIMPLES_NACIONAL -> Regimetributario.SimplesNacional;
            case LUCRO_PRESUMIDO -> Regimetributario.LucroPresumido;
            case LUCRO_REAL -> Regimetributario.LucroReal;
            default -> throw new IllegalArgumentException(
                    "Valor desconhecido para regime_tributario_tipo: " + dbData);
        };
    }
}
