package br.com.comandas.comanda;

import br.com.comandas.pagamento.PagamentoDtos;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class ComandaDtos {
    private ComandaDtos() { }

    public record Abertura(@NotBlank @Size(max = 160) @Schema(example = "Maria") String nomeCliente) { }
    public record InclusaoItem(@NotNull UUID variacaoId, @NotNull @Positive @Schema(example = "2") Integer quantidade) { }
    public record Quantidade(@NotNull @Positive @Schema(example = "3") Integer quantidade) { }
    public record ItemResposta(UUID id, UUID variacaoId, String nomeProduto, String nomeVariacao,
        BigDecimal precoUnitario, int quantidade, BigDecimal subtotal) { }
    public record Resumo(UUID id, String nomeCliente, StatusComanda status, Instant abertaEm,
        Instant finalizadaEm, BigDecimal total) { }
    public record Resposta(UUID id, String nomeCliente, StatusComanda status, Instant abertaEm,
        Instant finalizadaEm, BigDecimal total, List<ItemResposta> itens, PagamentoDtos.Resposta pagamento) { }
}

