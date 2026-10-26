package br.com.comandas.pagamento;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {
    boolean existsByComandaId(UUID comandaId);
    long countByComandaId(UUID comandaId);
}

