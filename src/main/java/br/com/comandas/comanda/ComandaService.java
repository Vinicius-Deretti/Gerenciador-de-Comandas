package br.com.comandas.comanda;

import br.com.comandas.categoria.CategoriaRepository;
import br.com.comandas.pagamento.PagamentoService;
import br.com.comandas.produto.*;
import br.com.comandas.shared.config.*;
import br.com.comandas.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ComandaService {
    private final ComandaRepository repository;
    private final ComandaItemRepository itens;
    private final ProdutoRepository produtos;
    private final ProdutoVariacaoRepository variacoes;
    private final CategoriaRepository categorias;
    private final PagamentoService pagamentos;
    private final ComandaMapper mapper;
    private final Clock clock;
    private final ZoneId fuso;

    public ComandaService(ComandaRepository repository, ComandaItemRepository itens, ProdutoRepository produtos,
        ProdutoVariacaoRepository variacoes, CategoriaRepository categorias, PagamentoService pagamentos,
        ComandaMapper mapper, Clock clock, ZoneId fuso) {
        this.repository = repository;
        this.itens = itens;
        this.produtos = produtos;
        this.variacoes = variacoes;
        this.categorias = categorias;
        this.pagamentos = pagamentos;
        this.mapper = mapper;
        this.clock = clock;
        this.fuso = fuso;
    }

    @Transactional
    public ComandaDtos.Resposta abrir(ComandaDtos.Abertura dto) {
        return mapper.toResponse(repository.save(new Comanda(dto.nomeCliente().strip(), clock.instant())));
    }

    public PageResponse<ComandaDtos.Resumo> abertas(int page, int size) {
        return PageResponse.from(repository.findByStatus(StatusComanda.ABERTA,
            PageRequest.of(page, size, Sort.by("abertaEm", "id"))).map(mapper::toResumo));
    }

    public ComandaDtos.Resposta consultar(UUID id) {
        return mapper.toResponse(repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Comanda")));
    }

    public ComandaDtos.ItemResposta consultarItem(UUID id, UUID itemId) {
        var comanda = repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Comanda"));
        return mapper.toResponse(buscarItem(comanda, itemId));
    }

    public PageResponse<ComandaDtos.Resumo> historico(LocalDate inicio, LocalDate fim, int page, int size) {
        if ((inicio == null) != (fim == null)) throw new EntradaInvalidaException("Informe inicio e fim juntos.");
        Specification<Comanda> spec = (root, query, cb) -> cb.equal(root.get("status"), StatusComanda.FINALIZADA);
        if (inicio != null) {
            var periodo = PeriodoDatas.of(inicio, fim, fuso);
            spec = spec.and((root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("finalizadaEm"), periodo.inicioInclusivo()),
                cb.lessThan(root.get("finalizadaEm"), periodo.fimExclusivo())));
        }
        return PageResponse.from(repository.findAll(spec,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "finalizadaEm", "id"))).map(mapper::toResumo));
    }

    @Transactional
    public ComandaDtos.Resposta incluirItem(UUID id, ComandaDtos.InclusaoItem dto) {
        var comanda = abertaComBloqueio(id);
        var produtoId = variacoes.buscarProdutoId(dto.variacaoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Variação"));
        // Mesma ordem de bloqueio do catálogo: produto -> categoria.
        var produto = produtos.buscarComBloqueio(produtoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Produto"));
        var categoria = categorias.buscarComBloqueio(produto.getCategoria().getId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria"));
        var variacao = variacoes.findById(dto.variacaoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Variação"));
        if (!produto.isAtivo() || !categoria.isAtivo() || !variacao.isAtivo()) {
            throw new RegraNegocioException("Produto, categoria e variação precisam estar ativos para novos lançamentos.");
        }
        var item = new ComandaItem(comanda, variacao, dto.quantidade(), subtotal(variacao.getPreco(), dto.quantidade()));
        comanda.adicionarItem(item);
        itens.save(item);
        recalcular(comanda);
        return mapper.toResponse(comanda);
    }

    @Transactional
    public ComandaDtos.Resposta alterarQuantidade(UUID id, UUID itemId, ComandaDtos.Quantidade dto) {
        var comanda = abertaComBloqueio(id);
        var item = buscarItem(comanda, itemId);
        item.alterarQuantidade(dto.quantidade(), subtotal(item.getPrecoUnitario(), dto.quantidade()));
        recalcular(comanda);
        return mapper.toResponse(comanda);
    }

    @Transactional
    public void removerItem(UUID id, UUID itemId) {
        var comanda = abertaComBloqueio(id);
        var item = buscarItem(comanda, itemId);
        comanda.removerItem(item);
        itens.delete(item);
        recalcular(comanda);
    }

    @Transactional
    public ComandaDtos.Resposta finalizar(UUID id) {
        var comanda = abertaComBloqueio(id);
        if (comanda.getItens().isEmpty()) throw new RegraNegocioException("A comanda precisa de pelo menos um item para ser finalizada.");
        recalcular(comanda);
        var agora = clock.instant();
        var pagamento = pagamentos.registrar(comanda, agora);
        comanda.finalizar(pagamento, agora);
        repository.flush();
        return mapper.toResponse(comanda);
    }

    private Comanda abertaComBloqueio(UUID id) {
        var comanda = repository.buscarComBloqueio(id).orElseThrow(() -> new RecursoNaoEncontradoException("Comanda"));
        if (comanda.getStatus() != StatusComanda.ABERTA) throw new RegraNegocioException("A comanda já está finalizada e não permite alterações.");
        return comanda;
    }

    private ComandaItem buscarItem(Comanda comanda, UUID itemId) {
        return comanda.getItens().stream().filter(i -> i.getId().equals(itemId)).findFirst()
            .orElseThrow(() -> new RecursoNaoEncontradoException("Item da comanda"));
    }

    private BigDecimal subtotal(BigDecimal preco, int quantidade) {
        if (quantidade <= 0) throw new EntradaInvalidaException("A quantidade deve ser maior que zero.");
        return Dinheiro.validarTotal(preco.multiply(BigDecimal.valueOf(quantidade)));
    }

    private void recalcular(Comanda comanda) {
        var total = comanda.getItens().stream().map(i -> subtotal(i.getPrecoUnitario(), i.getQuantidade()))
            .reduce(Dinheiro.ZERO, BigDecimal::add);
        comanda.atualizarTotal(Dinheiro.validarTotal(total));
    }
}
