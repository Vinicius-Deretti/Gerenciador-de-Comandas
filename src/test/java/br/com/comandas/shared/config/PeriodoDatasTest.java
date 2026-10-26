package br.com.comandas.shared.config;

import br.com.comandas.shared.exception.EntradaInvalidaException;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

class PeriodoDatasTest {
    @Test
    void datasInclusivasSaoConvertidasEmInstantesComFimExclusivo() {
        var periodo = PeriodoDatas.of(LocalDate.parse("2026-10-26"), LocalDate.parse("2026-10-26"),
            ZoneId.of("America/Sao_Paulo"));
        assertThat(periodo.inicioInclusivo()).isEqualTo(Instant.parse("2026-10-26T03:00:00Z"));
        assertThat(periodo.fimExclusivo()).isEqualTo(Instant.parse("2026-10-27T03:00:00Z"));
    }

    @Test
    void usaRegrasHistoricasDoFusoEmDiaComHorarioDeVerao() {
        var periodo = PeriodoDatas.of(LocalDate.parse("2018-11-04"), LocalDate.parse("2018-11-04"),
            ZoneId.of("America/Sao_Paulo"));
        assertThat(Duration.between(periodo.inicioInclusivo(), periodo.fimExclusivo())).isEqualTo(Duration.ofHours(23));
    }

    @Test
    void rejeitaPeriodoInvertido() {
        assertThatThrownBy(() -> PeriodoDatas.of(LocalDate.parse("2026-02-02"), LocalDate.parse("2026-02-01"), ZoneId.of("UTC")))
            .isInstanceOf(EntradaInvalidaException.class);
    }
}

