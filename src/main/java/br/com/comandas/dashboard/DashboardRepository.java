package br.com.comandas.dashboard;

import br.com.comandas.shared.config.PeriodoDatas;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;
import java.math.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.List;

@Repository
public class DashboardRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public DashboardRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public DashboardDtos.Totais totais(PeriodoDatas periodo) {
        return jdbc.queryForObject("""
            SELECT COALESCE(SUM(valor_pago), 0) AS receita, COUNT(*) AS quantidade
            FROM pagamento WHERE recebido_em >= :inicio AND recebido_em < :fim
            """, parametros(periodo), (rs, row) -> {
                var receita = rs.getBigDecimal("receita").setScale(2);
                var quantidade = rs.getLong("quantidade");
                return new DashboardDtos.Totais(receita, quantidade, media(receita, quantidade));
            });
    }

    public Page<DashboardDtos.Grupo> agrupar(PeriodoDatas periodo, ZoneId fuso, Agrupamento agrupamento, Pageable pageable) {
        var params = parametros(periodo).addValue("fuso", fuso.getId()).addValue("unidade", agrupamento.unidade())
            .addValue("limite", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        var total = jdbc.queryForObject("""
            SELECT COUNT(*) FROM (
                SELECT date_trunc(:unidade, recebido_em AT TIME ZONE :fuso)::date AS periodo
                FROM pagamento WHERE recebido_em >= :inicio AND recebido_em < :fim
                GROUP BY 1
            ) grupos
            """, params, Long.class);
        List<DashboardDtos.Grupo> grupos = jdbc.query("""
            SELECT date_trunc(:unidade, recebido_em AT TIME ZONE :fuso)::date AS periodo,
                   SUM(valor_pago) AS receita, COUNT(*) AS quantidade
            FROM pagamento WHERE recebido_em >= :inicio AND recebido_em < :fim
            GROUP BY 1 ORDER BY 1
            LIMIT :limite OFFSET :offset
            """, params, (rs, row) -> {
                var receita = rs.getBigDecimal("receita").setScale(2);
                var quantidade = rs.getLong("quantidade");
                return new DashboardDtos.Grupo(rs.getObject("periodo", LocalDate.class),
                    receita, quantidade, media(receita, quantidade));
            });
        return new PageImpl<>(grupos, pageable, total == null ? 0 : total);
    }

    private MapSqlParameterSource parametros(PeriodoDatas periodo) {
        return new MapSqlParameterSource("inicio", Timestamp.from(periodo.inicioInclusivo()))
            .addValue("fim", Timestamp.from(periodo.fimExclusivo()));
    }

    private BigDecimal media(BigDecimal receita, long quantidade) {
        return quantidade == 0 ? new BigDecimal("0.00")
            : receita.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);
    }
}

