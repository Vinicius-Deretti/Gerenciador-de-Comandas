package br.com.comandas.pagamento;

import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PagamentoMapper {
    PagamentoDtos.Resposta toResponse(Pagamento pagamento);
}

