package br.com.comandas.categoria;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class CategoriaDtos {
    private CategoriaDtos() { }

    public record Criacao(@NotBlank @Size(max = 120) @Schema(example = "Bebidas") String nome) { }
    public record Edicao(@NotBlank @Size(max = 120) String nome, @NotNull Boolean ativo) { }
    public record Resposta(UUID id, String nome, boolean ativo, Instant criadoEm, Instant atualizadoEm) { }
}

