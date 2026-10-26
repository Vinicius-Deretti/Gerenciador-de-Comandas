package br.com.comandas.comanda;

import br.com.comandas.produto.ProdutoVariacao;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "comanda_item")
public class ComandaItem {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comanda_id", nullable = false, updatable = false)
    private Comanda comanda;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variacao_id", nullable = false, updatable = false)
    private ProdutoVariacao variacao;
    @Column(nullable = false, length = 160, updatable = false)
    private String nomeProduto;
    @Column(length = 120, updatable = false)
    private String nomeVariacao;
    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal precoUnitario;
    @Column(nullable = false)
    private int quantidade;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    protected ComandaItem() { }

    public ComandaItem(Comanda comanda, ProdutoVariacao variacao, int quantidade, BigDecimal subtotal) {
        this.comanda = comanda;
        this.variacao = variacao;
        this.nomeProduto = variacao.getProduto().getNome();
        this.nomeVariacao = variacao.getNome();
        this.precoUnitario = variacao.getPreco();
        this.quantidade = quantidade;
        this.subtotal = subtotal;
    }

    public void alterarQuantidade(int quantidade, BigDecimal subtotal) {
        this.quantidade = quantidade;
        this.subtotal = subtotal;
    }

    public UUID getId() { return id; }
    public Comanda getComanda() { return comanda; }
    public ProdutoVariacao getVariacao() { return variacao; }
    public String getNomeProduto() { return nomeProduto; }
    public String getNomeVariacao() { return nomeVariacao; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public int getQuantidade() { return quantidade; }
    public BigDecimal getSubtotal() { return subtotal; }
}

