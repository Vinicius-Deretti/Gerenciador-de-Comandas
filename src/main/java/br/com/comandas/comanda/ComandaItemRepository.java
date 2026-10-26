package br.com.comandas.comanda;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ComandaItemRepository extends JpaRepository<ComandaItem, UUID> { }

