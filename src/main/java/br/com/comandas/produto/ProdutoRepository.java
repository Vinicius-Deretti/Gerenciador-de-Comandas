package br.com.comandas.produto;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface ProdutoRepository extends JpaRepository<Produto, UUID> {
    boolean existsByCategoriaIdAndAtivoTrue(UUID categoriaId);
    Page<Produto> findByAtivo(boolean ativo, Pageable pageable);
    Page<Produto> findByAtivoAndCategoriaId(boolean ativo, UUID categoriaId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produto p where p.id = :id")
    Optional<Produto> buscarComBloqueio(UUID id);
}

