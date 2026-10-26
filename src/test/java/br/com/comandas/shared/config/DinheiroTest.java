package br.com.comandas.shared.config;

import br.com.comandas.shared.exception.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class DinheiroTest {
    @Test
    void rejeitaPrecisaoExcedenteInclusiveZerosExtras() {
        assertThatThrownBy(() -> Dinheiro.preco(new BigDecimal("1.001"))).isInstanceOf(EntradaInvalidaException.class);
        assertThatThrownBy(() -> Dinheiro.preco(new BigDecimal("7.000"))).isInstanceOf(EntradaInvalidaException.class);
    }

    @Test
    void aceitaCentavosSemArredondar() {
        assertThat(Dinheiro.preco(new BigDecimal("0.01"))).isEqualTo(new BigDecimal("0.01"));
    }

    @Test
    void rejeitaTotalQueNaoCabeNoBanco() {
        assertThatThrownBy(() -> Dinheiro.validarTotal(new BigDecimal("10000000000.00"))).isInstanceOf(RegraNegocioException.class);
    }
}

