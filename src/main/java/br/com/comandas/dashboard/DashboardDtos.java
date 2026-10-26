package br.com.comandas.dashboard;

import br.com.comandas.shared.config.PageResponse;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class DashboardDtos {
    private DashboardDtos() { }

    public record Totais(BigDecimal receita, long comandasFinalizadas, BigDecimal ticketMedio) { }
    public record Grupo(LocalDate periodo, BigDecimal receita, long comandasFinalizadas, BigDecimal ticketMedio) { }
    public record Resposta(LocalDate inicio, LocalDate fim, String fuso, Agrupamento agrupamento,
        BigDecimal receita, long comandasFinalizadas, BigDecimal ticketMedio, PageResponse<Grupo> grupos) { }
}

