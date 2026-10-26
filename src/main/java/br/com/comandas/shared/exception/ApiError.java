package br.com.comandas.shared.exception;

import java.time.Instant;
import java.util.List;

public record ApiError(Instant timestamp, int status, String mensagem, String path, List<CampoInvalido> campos) {
    public record CampoInvalido(String campo, String mensagem) { }
}

