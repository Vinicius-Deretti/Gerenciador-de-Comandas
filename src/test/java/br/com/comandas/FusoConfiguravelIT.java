package br.com.comandas;

import br.com.comandas.categoria.*;
import br.com.comandas.produto.*;
import br.com.comandas.comanda.*;
import br.com.comandas.dashboard.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "estabelecimento.fuso=America/Manaus")
@Import(IntegrationSupport.TimeConfiguration.class)
class FusoConfiguravelIT extends IntegrationSupport {
    @Autowired CategoriaService categorias;
    @Autowired ProdutoService produtos;
    @Autowired ComandaService comandas;
    @Autowired DashboardService dashboard;
    @Autowired JdbcTemplate jdbc;
    @Autowired MutableClock clock;

    @Test
    void filtrosEAgrupamentosUsamFusoConfiguradoEmVezDoFusoDoServidor() {
        jdbc.execute("TRUNCATE pagamento, comanda_item, comanda, produto_variacao, produto, categoria");
        clock.set("2026-10-26T03:30:00Z"); // Ainda dia 25 em Manaus, já dia 26 em São Paulo.
        var categoria = categorias.criar(new CategoriaDtos.Criacao("Bebida"));
        var produto = produtos.criar(new ProdutoDtos.Criacao("Água", categoria.id(), null, null,
            List.of(new ProdutoDtos.VariacaoEntrada(null, null, new BigDecimal("5.00")))));
        var comanda = comandas.abrir(new ComandaDtos.Abertura("Maria"));
        comandas.incluirItem(comanda.id(), new ComandaDtos.InclusaoItem(produto.variacoes().get(0).id(), 1));
        comandas.finalizar(comanda.id());
        var dia25 = dashboard.diario(LocalDate.parse("2026-10-25"), 0, 20);
        assertThat(dia25.fuso()).isEqualTo("America/Manaus");
        assertThat(dia25.receita()).isEqualByComparingTo("5.00");
        assertThat(dia25.grupos().content().get(0).periodo()).isEqualTo(LocalDate.parse("2026-10-25"));
        assertThat(dashboard.diario(LocalDate.parse("2026-10-26"), 0, 20).receita()).isEqualByComparingTo("0.00");
        assertThat(comandas.historico(LocalDate.parse("2026-10-25"), LocalDate.parse("2026-10-25"), 0, 20).totalElements()).isEqualTo(1);
    }
}

