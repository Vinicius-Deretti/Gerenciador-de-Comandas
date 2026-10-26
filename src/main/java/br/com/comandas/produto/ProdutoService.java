package br.com.comandas.produto;

import br.com.comandas.categoria.*;
import br.com.comandas.shared.config.*;
import br.com.comandas.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ProdutoService {
    private final ProdutoRepository repository;
    private final CategoriaRepository categorias;
    private final ProdutoMapper mapper;
    private final Clock clock;

    public ProdutoService(ProdutoRepository repository, CategoriaRepository categorias, ProdutoMapper mapper, Clock clock) {
        this.repository = repository;
        this.categorias = categorias;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public ProdutoDtos.Resposta criar(ProdutoDtos.Criacao dto) {
        var categoria = categoriaAtiva(dto.categoriaId());
        var produto = new Produto(categoria, dto.nome().strip(), dto.descricao(), dto.imagemUrl(), clock.instant());
        substituirVariacoes(produto, dto.variacoes());
        return mapper.toResponse(repository.saveAndFlush(produto));
    }

    public ProdutoDtos.Resposta consultar(UUID id) {
        return mapper.toResponse(repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Produto")));
    }

    public PageResponse<ProdutoDtos.Resposta> listar(boolean ativo, UUID categoriaId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("nome", "id"));
        var produtos = categoriaId == null ? repository.findByAtivo(ativo, pageable)
            : repository.findByAtivoAndCategoriaId(ativo, categoriaId, pageable);
        return PageResponse.from(produtos.map(mapper::toResponse));
    }

    @Transactional
    public ProdutoDtos.Resposta editar(UUID id, ProdutoDtos.Edicao dto) {
        var produto = bloquear(id);
        var categoria = categorias.buscarComBloqueio(dto.categoriaId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria"));
        if (!categoria.isAtivo() && (dto.ativo() || !categoria.getId().equals(produto.getCategoria().getId()))) {
            throw new RegraNegocioException("Categorias inativas não podem receber produtos ativos ou novos produtos.");
        }
        if (dto.ativo()) validarOpcoes(produto.getVariacoes().stream()
            .filter(ProdutoVariacao::isAtivo).map(v -> new ProdutoDtos.VariacaoEntrada(v.getId(), v.getNome(), v.getPreco())).toList(), true);
        produto.atualizar(categoria, dto.nome().strip(), dto.descricao(), dto.imagemUrl(), dto.ativo(), clock.instant());
        return mapper.toResponse(produto);
    }

    @Transactional
    public ProdutoDtos.Resposta atualizarVariacoes(UUID id, ProdutoDtos.VariacoesEdicao dto) {
        var produto = bloquear(id);
        substituirVariacoes(produto, dto.variacoes());
        produto.registrarAtualizacao(clock.instant());
        repository.flush();
        return mapper.toResponse(produto);
    }

    @Transactional
    public void excluir(UUID id) {
        var produto = bloquear(id);
        produto.atualizar(produto.getCategoria(), produto.getNome(), produto.getDescricao(),
            produto.getImagemUrl(), false, clock.instant());
        produto.getVariacoes().forEach(ProdutoVariacao::desativar);
    }

    private Produto bloquear(UUID id) {
        return repository.buscarComBloqueio(id).orElseThrow(() -> new RecursoNaoEncontradoException("Produto"));
    }

    private Categoria categoriaAtiva(UUID id) {
        var categoria = categorias.buscarComBloqueio(id).orElseThrow(() -> new RecursoNaoEncontradoException("Categoria"));
        if (!categoria.isAtivo()) throw new RegraNegocioException("A categoria está inativa.");
        return categoria;
    }

    private void substituirVariacoes(Produto produto, List<ProdutoDtos.VariacaoEntrada> entradas) {
        validarOpcoes(entradas, produto.isAtivo());
        var existentes = new HashMap<UUID, ProdutoVariacao>();
        produto.getVariacoes().forEach(v -> existentes.put(v.getId(), v));
        var ids = new HashSet<UUID>();
        for (var entrada : entradas) {
            if (entrada.id() != null && (!ids.add(entrada.id()) || !existentes.containsKey(entrada.id()))) {
                throw new RegraNegocioException("ID de variação repetido ou que não pertence ao produto.");
            }
        }
        produto.getVariacoes().forEach(ProdutoVariacao::desativar);
        for (var entrada : entradas) {
            var nome = normalizarNome(entrada.nome());
            var preco = Dinheiro.preco(entrada.preco());
            if (entrada.id() == null) produto.adicionarVariacao(new ProdutoVariacao(produto, nome, preco));
            else existentes.get(entrada.id()).atualizar(nome, preco);
        }
    }

    private void validarOpcoes(List<ProdutoDtos.VariacaoEntrada> entradas, boolean ativo) {
        if (ativo && entradas.isEmpty()) throw new RegraNegocioException("Um produto ativo precisa de pelo menos uma variação ativa.");
        if (entradas.size() > 1 && entradas.stream().anyMatch(v -> normalizarNome(v.nome()) == null)) {
            throw new RegraNegocioException("Todas as variações ativas precisam de nome quando houver duas ou mais.");
        }
        entradas.forEach(v -> Dinheiro.preco(v.preco()));
    }

    private String normalizarNome(String nome) {
        return nome == null || nome.isBlank() ? null : nome.strip();
    }
}

