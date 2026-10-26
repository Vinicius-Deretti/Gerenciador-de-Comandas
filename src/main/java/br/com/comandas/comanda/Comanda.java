package br.com.comandas.comanda;

import br.com.comandas.pagamento.Pagamento;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "comanda")
public class Comanda {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 160)
    private String nomeCliente;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private StatusComanda status;
    @Column(nullable = false, updatable = false)
    private Instant abertaEm;
    private Instant finalizadaEm;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;
    @OneToMany(mappedBy = "comanda")
    @OrderBy("id")
    private List<ComandaItem> itens = new ArrayList<>();
    @OneToOne(mappedBy = "comanda", fetch = FetchType.LAZY)
    private Pagamento pagamento;

    protected Comanda() { }

    public Comanda(String nomeCliente, Instant agora) {
        this.nomeCliente = nomeCliente;
        this.status = StatusComanda.ABERTA;
        this.abertaEm = agora;
        this.total = new BigDecimal("0.00");
    }

    public void adicionarItem(ComandaItem item) { itens.add(item); }
    public void removerItem(ComandaItem item) { itens.remove(item); }
    public void atualizarTotal(BigDecimal total) { this.total = total; }
    public void finalizar(Pagamento pagamento, Instant agora) {
        this.pagamento = pagamento;
        this.status = StatusComanda.FINALIZADA;
        this.finalizadaEm = agora;
    }

    public UUID getId() { return id; }
    public String getNomeCliente() { return nomeCliente; }
    public StatusComanda getStatus() { return status; }
    public Instant getAbertaEm() { return abertaEm; }
    public Instant getFinalizadaEm() { return finalizadaEm; }
    public BigDecimal getTotal() { return total; }
    public List<ComandaItem> getItens() { return Collections.unmodifiableList(itens); }
    public Pagamento getPagamento() { return pagamento; }
}

