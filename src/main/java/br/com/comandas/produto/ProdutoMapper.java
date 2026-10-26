package br.com.comandas.produto;

import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProdutoMapper {
    @Mapping(target = "categoriaId", source = "categoria.id")
    ProdutoDtos.Resposta toResponse(Produto produto);
    ProdutoDtos.VariacaoResposta toResponse(ProdutoVariacao variacao);
}

