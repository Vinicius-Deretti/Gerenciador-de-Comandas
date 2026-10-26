package br.com.comandas.categoria;

import br.com.comandas.produto.ProdutoRepository;
import br.com.comandas.shared.config.PageResponse;
import br.com.comandas.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CategoriaService {
    private final CategoriaRepository repository;
    private final ProdutoRepository produtos;
    private final CategoriaMapper mapper;
    private final Clock clock;

    public CategoriaService(CategoriaRepository repository, ProdutoRepository produtos, CategoriaMapper mapper, Clock clock) {
        this.repository = repository;
        this.produtos = produtos;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public CategoriaDtos.Resposta criar(CategoriaDtos.Criacao dto) {
        return mapper.toResponse(repository.save(new Categoria(dto.nome().strip(), clock.instant())));
    }

    public CategoriaDtos.Resposta consultar(UUID id) {
        return mapper.toResponse(repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Categoria")));
    }

    public PageResponse<CategoriaDtos.Resposta> listar(boolean ativo, int page, int size) {
        return PageResponse.from(repository.findByAtivo(ativo,
            PageRequest.of(page, size, Sort.by("nome", "id"))).map(mapper::toResponse));
    }

    @Transactional
    public CategoriaDtos.Resposta editar(UUID id, CategoriaDtos.Edicao dto) {
        var categoria = bloquear(id);
        if (!dto.ativo()) validarDesativacao(id);
        categoria.atualizar(dto.nome().strip(), dto.ativo(), clock.instant());
        return mapper.toResponse(categoria);
    }

    @Transactional
    public void excluir(UUID id) {
        var categoria = bloquear(id);
        validarDesativacao(id);
        categoria.atualizar(categoria.getNome(), false, clock.instant());
    }

    private Categoria bloquear(UUID id) {
        return repository.buscarComBloqueio(id).orElseThrow(() -> new RecursoNaoEncontradoException("Categoria"));
    }

    private void validarDesativacao(UUID id) {
        if (produtos.existsByCategoriaIdAndAtivoTrue(id)) {
            throw new RegraNegocioException("A categoria possui produtos ativos e não pode ser desativada.");
        }
    }
}

