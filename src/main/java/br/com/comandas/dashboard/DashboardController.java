package br.com.comandas.dashboard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Receita por recebimento dos pagamentos no fuso configurado. Datas inicial e final inclusivas.")
public class DashboardController {
    private final DashboardService service;
    public DashboardController(DashboardService service) { this.service = service; }

    @GetMapping("/diario")
    @Operation(summary = "Receita diária", description = "data opcional: usa a data atual do estabelecimento.")
    public DashboardDtos.Resposta diario(@RequestParam(required = false) LocalDate data,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.diario(data, page, size);
    }

    @GetMapping("/semanal")
    @Operation(summary = "Receita da semana que contém a data", description = "Segunda-feira a domingo. data opcional.")
    public DashboardDtos.Resposta semanal(@RequestParam(required = false) LocalDate data,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.semanal(data, page, size);
    }

    @GetMapping("/mensal")
    @Operation(summary = "Receita do mês que contém a data", description = "Primeiro ao último dia do mês. data opcional.")
    public DashboardDtos.Resposta mensal(@RequestParam(required = false) LocalDate data,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.mensal(data, page, size);
    }

    @GetMapping("/periodo")
    @Operation(summary = "Receita, quantidade, ticket médio e valores agrupados por período",
        description = "Datas AAAA-MM-DD inclusivas. Agrupamento DIA, SEMANA ou MES. Grupos sem recebimentos são omitidos. "
            + "periodo identifica o início civil do grupo (segunda-feira para semanas). "
            + "Grupos nas bordas incluem somente pagamentos dentro do intervalo solicitado. Totais independem da página.")
    public DashboardDtos.Resposta periodo(@RequestParam LocalDate inicio, @RequestParam LocalDate fim,
        @RequestParam(defaultValue = "DIA") Agrupamento agrupamento,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.periodo(inicio, fim, agrupamento, page, size);
    }
}

