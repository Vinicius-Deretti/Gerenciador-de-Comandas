package br.com.comandas.categoria;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "categoria")
public class Categoria {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 120)
    private String nome;
    @Column(nullable = false)
    private boolean ativo;
    @Column(nullable = false, updatable = false)
    private Instant criadoEm;
    @Column(nullable = false)
    private Instant atualizadoEm;

    protected Categoria() { }

    public Categoria(String nome, Instant agora) {
        this.nome = nome;
        this.ativo = true;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    public void atualizar(String nome, boolean ativo, Instant agora) {
        this.nome = nome;
        this.ativo = ativo;
        this.atualizadoEm = agora;
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public boolean isAtivo() { return ativo; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
}

