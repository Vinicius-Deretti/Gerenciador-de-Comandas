package br.com.comandas.shared.exception;

public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String recurso) { super(recurso + " não encontrado(a)."); }
}

