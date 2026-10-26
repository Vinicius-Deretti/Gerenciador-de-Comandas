package br.com.comandas.comanda;

import br.com.comandas.pagamento.PagamentoMapper;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = PagamentoMapper.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ComandaMapper {
    ComandaDtos.Resposta toResponse(Comanda comanda);
    ComandaDtos.Resumo toResumo(Comanda comanda);
    @Mapping(target = "variacaoId", source = "variacao.id")
    ComandaDtos.ItemResposta toResponse(ComandaItem item);
}

