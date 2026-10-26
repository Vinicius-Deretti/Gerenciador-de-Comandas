package br.com.comandas.produto;

import br.com.comandas.shared.config.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/produtos")
@Tag(name = "Produtos")
public class ProdutoController {
    private final ProdutoService service;
    public ProdutoController(ProdutoService service) { this.service = service; }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Produto criado")
    @Operation(summary = "Criar produto com suas variações",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            examples = {
                @ExampleObject(name = "Um preço", value = "{\"nome\":\"Coxinha\",\"categoriaId\":\"f6dfe3f0-5ce1-414e-8f42-e33f20d92e9f\",\"variacoes\":[{\"preco\":7.00}]}"),
                @ExampleObject(name = "Vários preços", value = "{\"nome\":\"Velho Barreiro\",\"categoriaId\":\"f6dfe3f0-5ce1-414e-8f42-e33f20d92e9f\",\"variacoes\":[{\"nome\":\"Dose pequena\",\"preco\":2.50},{\"nome\":\"Dose grande\",\"preco\":5.00}]}")
            })))
    public ResponseEntity<ProdutoDtos.Resposta> criar(@Valid @RequestBody ProdutoDtos.Criacao dto) {
        var resposta = service.criar(dto);
        return ResponseEntity.created(URI.create("/api/v1/produtos/" + resposta.id())).body(resposta);
    }

    @GetMapping
    @Operation(summary = "Listar produtos", description = "Filtra por ativo (padrão true) e categoriaId opcional.")
    public PageResponse<ProdutoDtos.Resposta> listar(@RequestParam(defaultValue = "true") boolean ativo,
        @RequestParam(required = false) UUID categoriaId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.listar(ativo, categoriaId, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar produto com todas as variações, inclusive inativas")
    public ProdutoDtos.Resposta consultar(@PathVariable UUID id) { return service.consultar(id); }

    @PutMapping("/{id}")
    @Operation(summary = "Editar dados e estado do produto", description = "Reativação exige categoria ativa e pelo menos uma variação ativa.")
    public ProdutoDtos.Resposta editar(@PathVariable UUID id, @Valid @RequestBody ProdutoDtos.Edicao dto) {
        return service.editar(id, dto);
    }

    @PutMapping("/{id}/variacoes")
    @Operation(summary = "Substituir a coleção de variações ativas em uma transação",
        description = "Envie todas as opções desejadas. IDs existentes são preservados; sem ID cria nova opção; opções omitidas são desativadas. "
            + "Um ID inativo do mesmo produto pode ser reativado. Lista vazia só é aceita em produto inativo.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples =
            @ExampleObject(value = "{\"variacoes\":[{\"id\":\"dfb27976-02a7-4f2b-8275-1644fca6f3db\",\"nome\":\"Pequena\",\"preco\":7.00},{\"nome\":\"Grande\",\"preco\":12.00}]}"))))
    public ProdutoDtos.Resposta variacoes(@PathVariable UUID id, @Valid @RequestBody ProdutoDtos.VariacoesEdicao dto) {
        return service.atualizarVariacoes(id, dto);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204", description = "Produto e variações desativados")
    @Operation(summary = "Desativar produto e suas variações")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
