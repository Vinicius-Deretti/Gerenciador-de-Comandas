package br.com.comandas.pagamento;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class PagamentoDtos {
    private PagamentoDtos() { }
    public record Resposta(UUID id, BigDecimal valorPago, Instant recebidoEm) { }
}

