package br.com.comandas.shared.config;

import br.com.comandas.shared.exception.RegraNegocioException;
import br.com.comandas.shared.exception.EntradaInvalidaException;
import java.math.BigDecimal;

public final class Dinheiro {
    public static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal MAXIMO = new BigDecimal("9999999999.99");

    private Dinheiro() { }

    public static BigDecimal preco(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0 || valor.scale() > 2 || valor.compareTo(MAXIMO) > 0) {
            throw new EntradaInvalidaException("O preço deve ser positivo, com até 10 dígitos inteiros e duas casas decimais.");
        }
        return valor.setScale(2);
    }

    public static BigDecimal validarTotal(BigDecimal valor) {
        if (valor.signum() < 0 || valor.compareTo(MAXIMO) > 0) {
            throw new RegraNegocioException("O valor calculado excede o limite monetário de 9999999999.99.");
        }
        return valor.setScale(2, java.math.RoundingMode.UNNECESSARY);
    }
}

