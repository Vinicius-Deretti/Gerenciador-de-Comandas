package br.com.comandas.pagamento;

import br.com.comandas.comanda.Comanda;
import br.com.comandas.shared.exception.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Instant;

@Service
public class PagamentoService {
    private final PagamentoRepository repository;
    public PagamentoService(PagamentoRepository repository) { this.repository = repository; }

    // Só é chamado com a comanda bloqueada, dentro da transação de finalização.
    @Transactional(propagation = Propagation.MANDATORY)
    public Pagamento registrar(Comanda comanda, Instant agora) {
        if (repository.existsByComandaId(comanda.getId())) {
            throw new RegraNegocioException("A comanda já possui pagamento.");
        }
        return repository.save(new Pagamento(comanda, comanda.getTotal(), agora));
    }
}

