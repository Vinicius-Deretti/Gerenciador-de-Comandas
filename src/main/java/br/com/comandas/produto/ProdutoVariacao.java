package br.com.comandas.produto;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "produto_variacao")
public class ProdutoVariacao {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;
    @Column(length = 120)
    private String nome;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;
    @Column(nullable = false)
    private boolean ativo;

    protected ProdutoVariacao() { }

    public ProdutoVariacao(Produto produto, String nome, BigDecimal preco) {
        this.produto = produto;
        atualizar(nome, preco);
    }

    public void atualizar(String nome, BigDecimal preco) {
        this.nome = nome;
        this.preco = preco;
        this.ativo = true;
    }

    public void desativar() { ativo = false; }
    public UUID getId() { return id; }
    public Produto getProduto() { return produto; }
    public String getNome() { return nome; }
    public BigDecimal getPreco() { return preco; }
    public boolean isAtivo() { return ativo; }
}

