package br.com.comandas.pagamento;

import br.com.comandas.comanda.Comanda;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pagamento", uniqueConstraints = @UniqueConstraint(name = "uk_pagamento_comanda", columnNames = "comanda_id"))
public class Pagamento {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comanda_id", nullable = false, unique = true, updatable = false)
    private Comanda comanda;
    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal valorPago;
    @Column(nullable = false, updatable = false)
    private Instant recebidoEm;

    protected Pagamento() { }

    public Pagamento(Comanda comanda, BigDecimal valorPago, Instant recebidoEm) {
        this.comanda = comanda;
        this.valorPago = valorPago;
        this.recebidoEm = recebidoEm;
    }

    public UUID getId() { return id; }
    public Comanda getComanda() { return comanda; }
    public BigDecimal getValorPago() { return valorPago; }
    public Instant getRecebidoEm() { return recebidoEm; }
}

