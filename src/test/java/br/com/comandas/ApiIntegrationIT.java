package br.com.comandas;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationSupport.TimeConfiguration.class)
class ApiIntegrationIT extends IntegrationSupport {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired MutableClock clock;

    @BeforeEach
    void limpar() {
        jdbc.execute("TRUNCATE pagamento, comanda_item, comanda, produto_variacao, produto, categoria");
        clock.set("2026-10-26T15:00:00Z");
    }

    @Test
    void produtoComUmaVariacaoSemNome() throws Exception {
        var produto = produto("7.00");
        assertThat(produto.at("/variacoes/0/nome").isNull()).isTrue();
        assertThat(produto.at("/variacoes/0/ativo").asBoolean()).isTrue();
        assertThat(produto.at("/variacoes/0/id").asText()).isNotBlank();
        assertThat(produto.path("ativo").asBoolean()).isTrue();
    }

    @Test
    void multiplasVariacoesExigemNomesNaCriacaoEEdicao() throws Exception {
        var categoriaId = categoria();
        mvc.perform(post("/api/v1/produtos").contentType(MediaType.APPLICATION_JSON).content("""
            {"nome":"Bebida","categoriaId":"%s","variacoes":[{"preco":2.50},{"nome":"Grande","preco":5.00}]}
            """.formatted(categoriaId))).andExpect(status().isConflict());

        var produto = produto("7.00");
        var variacaoId = produto.at("/variacoes/0/id").asText();
        mvc.perform(put("/api/v1/produtos/" + produto.path("id").asText() + "/variacoes")
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"variacoes":[{"id":"%s","preco":8.00},{"nome":"Grande","preco":12.00}]}
                """.formatted(variacaoId))).andExpect(status().isConflict());
        var mantido = getJson("/api/v1/produtos/" + produto.path("id").asText());
        assertThat(mantido.at("/variacoes/0/preco").decimalValue()).isEqualByComparingTo("7.00");
        assertThat(mantido.path("variacoes").size()).isEqualTo(1);
    }

    @Test
    void adicionaSegundaVariacaoENomeiaPrimeiraPreservandoIdsEDesativandoRemovidas() throws Exception {
        var produto = produto("7.00");
        var id = produto.path("id").asText();
        var primeira = produto.at("/variacoes/0/id").asText();
        var editado = body(mvc.perform(put("/api/v1/produtos/" + id + "/variacoes")
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"variacoes":[{"id":"%s","nome":"Pequena","preco":7.00},{"nome":"Grande","preco":12.00}]}
                """.formatted(primeira))).andExpect(status().isOk()).andReturn());
        assertThat(editado.path("variacoes").size()).isEqualTo(2);
        assertThat(findVariacao(editado, primeira).path("nome").asText()).isEqualTo("Pequena");

        var unica = body(mvc.perform(put("/api/v1/produtos/" + id + "/variacoes")
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"variacoes":[{"id":"%s","preco":8.00}]}
                """.formatted(primeira))).andExpect(status().isOk()).andReturn());
        assertThat(findVariacao(unica, primeira).path("nome").isNull()).isTrue();
        assertThat(unica.path("variacoes").findValues("ativo").stream().filter(JsonNode::asBoolean).count()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM produto_variacao WHERE produto_id = ?::uuid", Integer.class, id)).isEqualTo(2);
    }

    @Test
    void produtoAtivoNaoPodeFicarSemVariacoesEReativacaoTambemValida() throws Exception {
        var produto = produto("7.00");
        var url = "/api/v1/produtos/" + produto.path("id").asText();
        mvc.perform(put(url + "/variacoes").contentType(MediaType.APPLICATION_JSON).content("{\"variacoes\":[]}"))
            .andExpect(status().isConflict());
        mvc.perform(delete(url)).andExpect(status().isNoContent());
        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content("""
            {"nome":"Coxinha","categoriaId":"%s","ativo":true}
            """.formatted(produto.path("categoriaId").asText()))).andExpect(status().isConflict());
        mvc.perform(put(url + "/variacoes").contentType(MediaType.APPLICATION_JSON).content("{\"variacoes\":[]}"))
            .andExpect(status().isOk());
    }

    @Test
    void exclusaoDeCategoriaComProdutosAtivosEProibidaECategoriaInativaNaoRecebeProdutos() throws Exception {
        var produto = produto("7.00");
        var categoriaId = produto.path("categoriaId").asText();
        mvc.perform(delete("/api/v1/categorias/" + categoriaId)).andExpect(status().isConflict());
        mvc.perform(put("/api/v1/categorias/" + categoriaId).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\"Salgados\",\"ativo\":false}")).andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/produtos/" + produto.path("id").asText())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/categorias/" + categoriaId)).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/produtos").contentType(MediaType.APPLICATION_JSON).content("""
            {"nome":"Novo","categoriaId":"%s","variacoes":[{"preco":3.00}]}
            """.formatted(categoriaId))).andExpect(status().isConflict());
    }

    @Test
    void preservaPrecoENomesHistoricosAposEdicaoEDesativacao() throws Exception {
        var produto = produto("7.00");
        var variacaoId = produto.at("/variacoes/0/id").asText();
        var comandaId = abrir();
        var item = incluir(comandaId, variacaoId, 2).at("/itens/0/id").asText();
        var urlProduto = "/api/v1/produtos/" + produto.path("id").asText();
        mvc.perform(put(urlProduto + "/variacoes").contentType(MediaType.APPLICATION_JSON).content("""
            {"variacoes":[{"id":"%s","nome":"Novo nome","preco":9.00}]}
            """.formatted(variacaoId))).andExpect(status().isOk());
        mvc.perform(put(urlProduto).contentType(MediaType.APPLICATION_JSON).content("""
            {"nome":"Outro produto","categoriaId":"%s","ativo":true}
            """.formatted(produto.path("categoriaId").asText()))).andExpect(status().isOk());
        mvc.perform(delete(urlProduto)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/categorias/" + produto.path("categoriaId").asText())).andExpect(status().isNoContent());
        var alterada = body(mvc.perform(patch("/api/v1/comandas/" + comandaId + "/itens/" + item)
            .contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":3}"))
            .andExpect(status().isOk()).andReturn());
        assertThat(alterada.at("/itens/0/precoUnitario").decimalValue()).isEqualByComparingTo("7.00");
        assertThat(alterada.at("/itens/0/nomeProduto").asText()).isEqualTo("Coxinha");
        assertThat(alterada.at("/itens/0/nomeVariacao").isNull()).isTrue();
        assertThat(alterada.path("total").decimalValue()).isEqualByComparingTo("21.00");
        mvc.perform(post("/api/v1/comandas/" + comandaId + "/itens").contentType(MediaType.APPLICATION_JSON)
            .content("{\"variacaoId\":\"" + variacaoId + "\",\"quantidade\":1}")).andExpect(status().isConflict());
        finalizar(comandaId);
    }

    @Test
    void variacaoInativaNaoPodeSerLancadaMesmoComProdutoAtivo() throws Exception {
        var produto = produto("7.00");
        var antiga = produto.at("/variacoes/0/id").asText();
        mvc.perform(put("/api/v1/produtos/" + produto.path("id").asText() + "/variacoes")
            .contentType(MediaType.APPLICATION_JSON).content("{\"variacoes\":[{\"preco\":8.00}]}")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/comandas/" + abrir() + "/itens").contentType(MediaType.APPLICATION_JSON)
            .content("{\"variacaoId\":\"" + antiga + "\",\"quantidade\":1}")).andExpect(status().isConflict());
    }

    @Test
    void comandaFinalizadaBloqueiaAlteracoesEFinalizacaoRepetida() throws Exception {
        var produto = produto("7.00");
        var comandaId = abrir();
        var variacaoId = produto.at("/variacoes/0/id").asText();
        var item = incluir(comandaId, variacaoId, 2).at("/itens/0/id").asText();
        var finalizada = finalizar(comandaId);
        assertThat(finalizada.path("status").asText()).isEqualTo("FINALIZADA");
        assertThat(finalizada.at("/pagamento/valorPago").decimalValue()).isEqualByComparingTo("14.00");
        mvc.perform(patch("/api/v1/comandas/" + comandaId + "/itens/" + item).contentType(MediaType.APPLICATION_JSON)
            .content("{\"quantidade\":3}")).andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/comandas/" + comandaId + "/itens/" + item)).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/comandas/" + comandaId + "/itens").contentType(MediaType.APPLICATION_JSON)
            .content("{\"variacaoId\":\"" + variacaoId + "\",\"quantidade\":1}")).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/comandas/" + comandaId + "/finalizacao")).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pagamento", Integer.class)).isEqualTo(1);
        assertThat(getJson("/api/v1/comandas").path("totalElements").asInt()).isZero();
        assertThat(getJson("/api/v1/comandas/historico").path("totalElements").asInt()).isEqualTo(1);
    }

    @Test
    void comandaVaziaNaoFinalizaERemocaoRecalculaTotal() throws Exception {
        var id = abrir();
        var produto = produto("7.00");
        var item = incluir(id, produto.at("/variacoes/0/id").asText(), 2).at("/itens/0/id").asText();
        mvc.perform(delete("/api/v1/comandas/" + id + "/itens/" + item)).andExpect(status().isNoContent());
        assertThat(getJson("/api/v1/comandas/" + id).path("total").decimalValue()).isEqualByComparingTo("0.00");
        mvc.perform(post("/api/v1/comandas/" + id + "/finalizacao")).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pagamento", Integer.class)).isZero();
    }

    @Test
    void falhaNoPagamentoReverteFinalizacaoInteira() throws Exception {
        var produto = produto("7.00");
        var id = abrir();
        incluir(id, produto.at("/variacoes/0/id").asText(), 1);
        jdbc.execute("""
            CREATE FUNCTION falhar_pagamento() RETURNS trigger LANGUAGE plpgsql AS $$
            BEGIN RAISE EXCEPTION 'Falha de teste' USING ERRCODE = '23514'; END; $$
            """);
        jdbc.execute("CREATE TRIGGER falhar_pagamento BEFORE INSERT ON pagamento FOR EACH ROW EXECUTE FUNCTION falhar_pagamento()");
        try {
            mvc.perform(post("/api/v1/comandas/" + id + "/finalizacao")).andExpect(status().isConflict());
            var comanda = getJson("/api/v1/comandas/" + id);
            assertThat(comanda.path("status").asText()).isEqualTo("ABERTA");
            assertThat(comanda.path("finalizadaEm").isNull()).isTrue();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM pagamento", Integer.class)).isZero();
        } finally {
            jdbc.execute("DROP TRIGGER falhar_pagamento ON pagamento");
            jdbc.execute("DROP FUNCTION falhar_pagamento()");
        }
        finalizar(id);
    }

    @Test
    void finalizacoesSimultaneasCriamUmUnicoPagamento() throws Exception {
        var produto = produto("7.00");
        var id = abrir();
        incluir(id, produto.at("/variacoes/0/id").asText(), 2);
        var inicio = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> finalizar = () -> {
                inicio.await();
                return mvc.perform(post("/api/v1/comandas/" + id + "/finalizacao")).andReturn().getResponse().getStatus();
            };
            var primeira = executor.submit(finalizar);
            var segunda = executor.submit(finalizar);
            inicio.countDown();
            assertThat(List.of(primeira.get(20, TimeUnit.SECONDS), segunda.get(20, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(200, 409);
        } finally {
            executor.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pagamento", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT valor_pago FROM pagamento", BigDecimal.class)).isEqualByComparingTo("14.00");
    }

    @Test
    void quantidadeConcorrenteComFinalizacaoMantemPagamentoIgualAoTotal() throws Exception {
        var produto = produto("7.00");
        var id = abrir();
        var item = incluir(id, produto.at("/variacoes/0/id").asText(), 2).at("/itens/0/id").asText();
        var inicio = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var alteracao = executor.submit(() -> {
                inicio.await();
                return mvc.perform(patch("/api/v1/comandas/" + id + "/itens/" + item).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"quantidade\":3}")).andReturn().getResponse().getStatus();
            });
            var finalizacao = executor.submit(() -> {
                inicio.await();
                return mvc.perform(post("/api/v1/comandas/" + id + "/finalizacao")).andReturn().getResponse().getStatus();
            });
            inicio.countDown();
            var statusAlteracao = alteracao.get(20, TimeUnit.SECONDS);
            assertThat(statusAlteracao).isIn(200, 409);
            assertThat(finalizacao.get(20, TimeUnit.SECONDS)).isEqualTo(200);
            var esperado = statusAlteracao == 200 ? "21.00" : "14.00";
            var comanda = getJson("/api/v1/comandas/" + id);
            assertThat(comanda.path("total").decimalValue()).isEqualByComparingTo(esperado);
            assertThat(comanda.at("/pagamento/valorPago").decimalValue()).isEqualByComparingTo(esperado);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void dashboardUsaRecebimentoDesconsideraAbertasERespeitaFronteirasDoFuso() throws Exception {
        var produto = produto("7.00");
        var variacao = produto.at("/variacoes/0/id").asText();
        clock.set("2026-10-26T02:59:59Z");
        var foraAntes = abrir(); incluir(foraAntes, variacao, 1); finalizar(foraAntes);
        clock.set("2026-10-26T03:00:00Z");
        var dentroInicio = abrir(); incluir(dentroInicio, variacao, 2); finalizar(dentroInicio);
        clock.set("2026-10-27T02:59:59Z");
        var dentroFim = abrir(); incluir(dentroFim, variacao, 3); finalizar(dentroFim);
        clock.set("2026-10-27T03:00:00Z");
        var foraDepois = abrir(); incluir(foraDepois, variacao, 4); finalizar(foraDepois);
        incluir(abrir(), variacao, 100);

        var diario = getJson("/api/v1/dashboard/diario?data=2026-10-26");
        assertThat(diario.path("receita").decimalValue()).isEqualByComparingTo("35.00");
        assertThat(diario.path("comandasFinalizadas").asInt()).isEqualTo(2);
        assertThat(diario.path("ticketMedio").decimalValue()).isEqualByComparingTo("17.50");
        assertThat(diario.at("/grupos/content/0/periodo").asText()).isEqualTo("2026-10-26");
        assertThat(getJson("/api/v1/comandas/historico?inicio=2026-10-26&fim=2026-10-26").path("totalElements").asInt()).isEqualTo(2);
        // A data de recebimento, e não a de finalização, determina a receita.
        jdbc.update("UPDATE pagamento SET recebido_em = '2026-10-25T03:00:00Z' WHERE comanda_id = ?::uuid", dentroInicio);
        assertThat(getJson("/api/v1/dashboard/diario?data=2026-10-26").path("receita").decimalValue()).isEqualByComparingTo("21.00");
    }

    @Test
    void agrupamentosSemanaisMensaisEPaginacaoNaoAlteramTotais() throws Exception {
        var produto = produto("10.00");
        var variacao = produto.at("/variacoes/0/id").asText();
        for (var data : List.of("2026-12-28T12:00:00Z", "2027-01-03T12:00:00Z", "2027-01-04T12:00:00Z")) {
            clock.set(data);
            var id = abrir(); incluir(id, variacao, 1); finalizar(id);
        }
        var semanal = getJson("/api/v1/dashboard/periodo?inicio=2026-12-28&fim=2027-01-04&agrupamento=SEMANA&size=1&page=1");
        assertThat(semanal.path("receita").decimalValue()).isEqualByComparingTo("30.00");
        assertThat(semanal.at("/grupos/totalElements").asInt()).isEqualTo(2);
        assertThat(semanal.at("/grupos/content/0/periodo").asText()).isEqualTo("2027-01-04");
        var semana = getJson("/api/v1/dashboard/semanal?data=2027-01-03");
        assertThat(semana.path("inicio").asText()).isEqualTo("2026-12-28");
        assertThat(semana.path("fim").asText()).isEqualTo("2027-01-03");
        assertThat(semana.path("receita").decimalValue()).isEqualByComparingTo("20.00");
        var mensal = getJson("/api/v1/dashboard/mensal?data=2027-01-20");
        assertThat(mensal.path("receita").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(getJson("/api/v1/dashboard/periodo?inicio=2026-12-01&fim=2027-01-31&agrupamento=MES")
            .at("/grupos/totalElements").asInt()).isEqualTo(2);
    }

    @Test
    void validaEntradasPrecisaoCamposDesconhecidosEStatusHttp() throws Exception {
        mvc.perform(post("/api/v1/categorias").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\" \"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos[0].campo").value("nome"));
        var categoria = categoria();
        for (String preco : List.of("7.001", "7.000", "0", "-1")) {
            mvc.perform(post("/api/v1/produtos").contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Produto","categoriaId":"%s","variacoes":[{"preco":%s}]}
                """.formatted(categoria, preco))).andExpect(status().isBadRequest());
        }
        var produto = produto("7.00");
        var id = abrir();
        for (String quantidade : List.of("0", "-1", "1.5", "null")) {
            mvc.perform(post("/api/v1/comandas/" + id + "/itens").contentType(MediaType.APPLICATION_JSON).content("""
                {"variacaoId":"%s","quantidade":%s}
                """.formatted(produto.at("/variacoes/0/id").asText(), quantidade))).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/v1/comandas/" + id + "/itens").contentType(MediaType.APPLICATION_JSON).content("""
            {"variacaoId":"%s","quantidade":1,"precoUnitario":0.01}
            """.formatted(produto.at("/variacoes/0/id").asText()))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/comandas?page=-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/comandas?size=101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/comandas/" + UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/dashboard/periodo?inicio=2026-10-27&fim=2026-10-26")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/comandas/historico?inicio=2026-10-26")).andExpect(status().isBadRequest());
    }

    @Test
    void swaggerDocumentaEndpointsEExemplos() throws Exception {
        var docs = getJson("/v3/api-docs");
        assertThat(docs.at("/paths/~1api~1v1~1produtos/post/requestBody/content/application~1json/examples").size()).isEqualTo(2);
        assertThat(docs.at("/paths/~1api~1v1~1comandas~1{id}~1finalizacao/post").isMissingNode()).isFalse();
        assertThat(docs.at("/paths/~1api~1v1~1produtos/post/responses/201").isMissingNode()).isFalse();
        assertThat(docs.at("/paths/~1api~1v1~1produtos/post/responses/409").isMissingNode()).isFalse();
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    private String categoria() throws Exception {
        return body(mvc.perform(post("/api/v1/categorias").contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\"Salgados\"}")).andExpect(status().isCreated()).andReturn()).path("id").asText();
    }

    private JsonNode produto(String preco) throws Exception {
        return body(mvc.perform(post("/api/v1/produtos").contentType(MediaType.APPLICATION_JSON).content("""
            {"nome":"Coxinha","categoriaId":"%s","variacoes":[{"preco":%s}]}
            """.formatted(categoria(), preco))).andExpect(status().isCreated()).andReturn());
    }

    private String abrir() throws Exception {
        return body(mvc.perform(post("/api/v1/comandas").contentType(MediaType.APPLICATION_JSON)
            .content("{\"nomeCliente\":\"Maria\"}")).andExpect(status().isCreated()).andReturn()).path("id").asText();
    }

    private JsonNode incluir(String id, String variacao, int quantidade) throws Exception {
        var resultado = mvc.perform(post("/api/v1/comandas/" + id + "/itens").contentType(MediaType.APPLICATION_JSON)
            .content("{\"variacaoId\":\"" + variacao + "\",\"quantidade\":" + quantidade + "}"))
            .andExpect(status().isCreated()).andReturn();
        mvc.perform(get(resultado.getResponse().getHeader("Location"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.variacaoId").value(variacao));
        return body(resultado);
    }

    private JsonNode finalizar(String id) throws Exception {
        return body(mvc.perform(post("/api/v1/comandas/" + id + "/finalizacao")).andExpect(status().isOk()).andReturn());
    }

    private JsonNode getJson(String url) throws Exception {
        return body(mvc.perform(get(url)).andExpect(status().isOk()).andReturn());
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private JsonNode findVariacao(JsonNode produto, String id) {
        for (var variacao : produto.path("variacoes")) if (variacao.path("id").asText().equals(id)) return variacao;
        throw new AssertionError("Variação não encontrada: " + id);
    }
}
