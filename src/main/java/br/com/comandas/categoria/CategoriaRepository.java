package br.com.comandas.categoria;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {
    Page<Categoria> findByAtivo(boolean ativo, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Categoria c where c.id = :id")
    Optional<Categoria> buscarComBloqueio(UUID id);
}

