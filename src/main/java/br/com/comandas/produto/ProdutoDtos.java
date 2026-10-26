package br.com.comandas.produto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ProdutoDtos {
    private ProdutoDtos() { }

    public record VariacaoEntrada(
        @Schema(description = "Omitir para nova opção. Para editar ou reativar, usar ID deste produto.") UUID id,
        @Size(max = 120) @Schema(example = "Dose pequena", description = "Opcional quando há uma única opção ativa.") String nome,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 10, fraction = 2)
        @Schema(example = "2.50") BigDecimal preco) { }

    public record Criacao(
        @NotBlank @Size(max = 160) @Schema(example = "Coxinha") String nome,
        @NotNull UUID categoriaId,
        @Size(max = 2000) String descricao,
        @Size(max = 2048) @Pattern(regexp = "https?://[^\\s]+", message = "deve ser uma URL HTTP ou HTTPS") String imagemUrl,
        @NotEmpty List<@NotNull @Valid VariacaoEntrada> variacoes) { }

    public record Edicao(
        @NotBlank @Size(max = 160) String nome,
        @NotNull UUID categoriaId,
        @Size(max = 2000) String descricao,
        @Size(max = 2048) @Pattern(regexp = "https?://[^\\s]+", message = "deve ser uma URL HTTP ou HTTPS") String imagemUrl,
        @NotNull Boolean ativo) { }

    public record VariacoesEdicao(@NotNull List<@NotNull @Valid VariacaoEntrada> variacoes) { }
    public record VariacaoResposta(UUID id, String nome, BigDecimal preco, boolean ativo) { }
    public record Resposta(UUID id, UUID categoriaId, String nome, String descricao, String imagemUrl,
        boolean ativo, Instant criadoEm, Instant atualizadoEm, List<VariacaoResposta> variacoes) { }
}

