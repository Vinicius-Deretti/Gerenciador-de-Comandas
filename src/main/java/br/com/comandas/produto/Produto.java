package br.com.comandas.produto;

import br.com.comandas.categoria.Categoria;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "produto")
public class Produto {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;
    @Column(nullable = false, length = 160)
    private String nome;
    @Column(length = 2000)
    private String descricao;
    @Column(length = 2048)
    private String imagemUrl;
    @Column(nullable = false)
    private boolean ativo;
    @Column(nullable = false, updatable = false)
    private Instant criadoEm;
    @Column(nullable = false)
    private Instant atualizadoEm;
    @OneToMany(mappedBy = "produto", cascade = CascadeType.PERSIST)
    @OrderBy("id")
    private List<ProdutoVariacao> variacoes = new ArrayList<>();

    protected Produto() { }

    public Produto(Categoria categoria, String nome, String descricao, String imagemUrl, Instant agora) {
        this.categoria = categoria;
        this.nome = nome;
        this.descricao = descricao;
        this.imagemUrl = imagemUrl;
        this.ativo = true;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    public void atualizar(Categoria categoria, String nome, String descricao, String imagemUrl, boolean ativo, Instant agora) {
        this.categoria = categoria;
        this.nome = nome;
        this.descricao = descricao;
        this.imagemUrl = imagemUrl;
        this.ativo = ativo;
        this.atualizadoEm = agora;
    }

    public void adicionarVariacao(ProdutoVariacao variacao) { variacoes.add(variacao); }
    public void registrarAtualizacao(Instant agora) { atualizadoEm = agora; }

    public UUID getId() { return id; }
    public Categoria getCategoria() { return categoria; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getImagemUrl() { return imagemUrl; }
    public boolean isAtivo() { return ativo; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public List<ProdutoVariacao> getVariacoes() { return Collections.unmodifiableList(variacoes); }
}

