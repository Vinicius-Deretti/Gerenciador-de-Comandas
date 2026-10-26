package br.com.comandas.produto;

import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface ProdutoVariacaoRepository extends JpaRepository<ProdutoVariacao, UUID> {
    @Query("select v.produto.id from ProdutoVariacao v where v.id = :id")
    Optional<UUID> buscarProdutoId(UUID id);
}

