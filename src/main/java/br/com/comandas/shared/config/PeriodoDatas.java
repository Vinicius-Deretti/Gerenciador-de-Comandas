package br.com.comandas.shared.config;

import br.com.comandas.shared.exception.EntradaInvalidaException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public record PeriodoDatas(LocalDate inicio, LocalDate fim, Instant inicioInclusivo, Instant fimExclusivo) {
    public static PeriodoDatas of(LocalDate inicio, LocalDate fim, ZoneId fuso) {
        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new EntradaInvalidaException("Informe inicio e fim válidos, com fim maior ou igual ao inicio.");
        }
        // Evita overflow no plusDays e nos limites representáveis de timestamps.
        if (inicio.getYear() < 1 || fim.getYear() > 9998) {
            throw new EntradaInvalidaException("As datas devem estar entre os anos 1 e 9998.");
        }
        return new PeriodoDatas(inicio, fim, inicio.atStartOfDay(fuso).toInstant(),
            fim.plusDays(1).atStartOfDay(fuso).toInstant());
    }
}

