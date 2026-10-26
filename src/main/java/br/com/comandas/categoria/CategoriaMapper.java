package br.com.comandas.categoria;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CategoriaMapper {
    CategoriaDtos.Resposta toResponse(Categoria categoria);
}

