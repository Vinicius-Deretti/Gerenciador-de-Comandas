package br.com.comandas.comanda;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface ComandaRepository extends JpaRepository<Comanda, UUID>, JpaSpecificationExecutor<Comanda> {
    Page<Comanda> findByStatus(StatusComanda status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Comanda c where c.id = :id")
    Optional<Comanda> buscarComBloqueio(UUID id);
}

