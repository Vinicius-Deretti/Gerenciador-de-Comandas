package br.com.comandas.dashboard;

import br.com.comandas.shared.config.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class DashboardService {
    private final DashboardRepository repository;
    private final ZoneId fuso;
    private final Clock clock;

    public DashboardService(DashboardRepository repository, ZoneId fuso, Clock clock) {
        this.repository = repository;
        this.fuso = fuso;
        this.clock = clock;
    }

    public DashboardDtos.Resposta diario(LocalDate data, int page, int size) {
        var dia = referencia(data);
        return periodo(dia, dia, Agrupamento.DIA, page, size);
    }

    public DashboardDtos.Resposta semanal(LocalDate data, int page, int size) {
        var segunda = referencia(data).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return periodo(segunda, segunda.plusDays(6), Agrupamento.DIA, page, size);
    }

    public DashboardDtos.Resposta mensal(LocalDate data, int page, int size) {
        var mes = YearMonth.from(referencia(data));
        return periodo(mes.atDay(1), mes.atEndOfMonth(), Agrupamento.DIA, page, size);
    }

    public DashboardDtos.Resposta periodo(LocalDate inicio, LocalDate fim, Agrupamento agrupamento, int page, int size) {
        var periodo = PeriodoDatas.of(inicio, fim, fuso);
        var totais = repository.totais(periodo);
        var grupos = repository.agrupar(periodo, fuso, agrupamento, PageRequest.of(page, size));
        return new DashboardDtos.Resposta(inicio, fim, fuso.getId(), agrupamento,
            totais.receita(), totais.comandasFinalizadas(), totais.ticketMedio(), PageResponse.from(grupos));
    }

    private LocalDate referencia(LocalDate data) { return data == null ? LocalDate.now(clock.withZone(fuso)) : data; }
}

