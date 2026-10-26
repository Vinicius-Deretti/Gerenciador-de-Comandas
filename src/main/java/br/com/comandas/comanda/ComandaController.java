package br.com.comandas.comanda;

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
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/comandas")
@Tag(name = "Comandas")
public class ComandaController {
    private final ComandaService service;
    public ComandaController(ComandaService service) { this.service = service; }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Comanda aberta")
    @Operation(summary = "Abrir comanda", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = @Content(examples = @ExampleObject(value = "{\"nomeCliente\":\"Maria\"}"))))
    public ResponseEntity<ComandaDtos.Resposta> abrir(@Valid @RequestBody ComandaDtos.Abertura dto) {
        var resposta = service.abrir(dto);
        return ResponseEntity.created(URI.create("/api/v1/comandas/" + resposta.id())).body(resposta);
    }

    @GetMapping
    @Operation(summary = "Listar somente comandas abertas para a tela inicial")
    public PageResponse<ComandaDtos.Resumo> abertas(@RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.abertas(page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar comanda com itens, total e pagamento")
    public ComandaDtos.Resposta consultar(@PathVariable UUID id) { return service.consultar(id); }

    @GetMapping("/historico")
    @Operation(summary = "Histórico paginado de comandas finalizadas",
        description = "inicio e fim opcionais, informados juntos. Ambas as datas são inclusivas no fuso do estabelecimento.")
    public PageResponse<ComandaDtos.Resumo> historico(@RequestParam(required = false) LocalDate inicio,
        @RequestParam(required = false) LocalDate fim, @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.historico(inicio, fim, page, size);
    }

    @PostMapping("/{id}/itens")
    @ApiResponse(responseCode = "201", description = "Item incluído; retorna comanda atualizada")
    @Operation(summary = "Adicionar item com preço atual do catálogo", description = "Cada inclusão cria um item, preservando nome e preço históricos.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples =
            @ExampleObject(value = "{\"variacaoId\":\"dfb27976-02a7-4f2b-8275-1644fca6f3db\",\"quantidade\":2}"))))
    public ResponseEntity<ComandaDtos.Resposta> incluir(@PathVariable UUID id, @Valid @RequestBody ComandaDtos.InclusaoItem dto) {
        var resposta = service.incluirItem(id, dto);
        var itemId = resposta.itens().get(resposta.itens().size() - 1).id();
        return ResponseEntity.created(URI.create("/api/v1/comandas/" + id + "/itens/" + itemId)).body(resposta);
    }

    @PatchMapping("/{id}/itens/{itemId}")
    @Operation(summary = "Alterar quantidade mantendo o preço histórico")
    public ComandaDtos.Resposta quantidade(@PathVariable UUID id, @PathVariable UUID itemId,
        @Valid @RequestBody ComandaDtos.Quantidade dto) {
        return service.alterarQuantidade(id, itemId, dto);
    }

    @GetMapping("/{id}/itens/{itemId}")
    @Operation(summary = "Consultar um item da comanda")
    public ComandaDtos.ItemResposta consultarItem(@PathVariable UUID id, @PathVariable UUID itemId) {
        return service.consultarItem(id, itemId);
    }

    @DeleteMapping("/{id}/itens/{itemId}")
    @ApiResponse(responseCode = "204", description = "Item removido")
    @Operation(summary = "Remover item de comanda aberta")
    public ResponseEntity<Void> remover(@PathVariable UUID id, @PathVariable UUID itemId) {
        service.removerItem(id, itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/finalizacao")
    @Operation(summary = "Finalizar comanda e registrar pagamento integral",
        description = "Sem corpo. O backend calcula o valor. Operação atômica; comanda já finalizada retorna 409 e nenhum pagamento adicional.")
    public ComandaDtos.Resposta finalizar(@PathVariable UUID id) { return service.finalizar(id); }
}
