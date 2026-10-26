package br.com.comandas.categoria;

import br.com.comandas.shared.config.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categorias")
public class CategoriaController {
    private final CategoriaService service;
    public CategoriaController(CategoriaService service) { this.service = service; }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Categoria criada")
    @Operation(summary = "Criar categoria")
    public ResponseEntity<CategoriaDtos.Resposta> criar(@Valid @RequestBody CategoriaDtos.Criacao dto) {
        var resposta = service.criar(dto);
        return ResponseEntity.created(URI.create("/api/v1/categorias/" + resposta.id())).body(resposta);
    }

    @GetMapping
    @Operation(summary = "Listar categorias", description = "ativo=true por padrão; ativo=false consulta as desativadas.")
    public PageResponse<CategoriaDtos.Resposta> listar(@RequestParam(defaultValue = "true") boolean ativo,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.listar(ativo, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar categoria, inclusive inativa")
    public CategoriaDtos.Resposta consultar(@PathVariable UUID id) { return service.consultar(id); }

    @PutMapping("/{id}")
    @Operation(summary = "Editar ou reativar categoria")
    public CategoriaDtos.Resposta editar(@PathVariable UUID id, @Valid @RequestBody CategoriaDtos.Edicao dto) {
        return service.editar(id, dto);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204", description = "Categoria desativada")
    @Operation(summary = "Desativar categoria sem produtos ativos")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
