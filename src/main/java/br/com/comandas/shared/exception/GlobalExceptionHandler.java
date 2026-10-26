package br.com.comandas.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.*;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ResponseEntity<ApiError> naoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest req) {
        return erro(404, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(RegraNegocioException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ResponseEntity<ApiError> regra(RegraNegocioException ex, HttpServletRequest req) {
        return erro(409, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(EntradaInvalidaException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ResponseEntity<ApiError> entrada(EntradaInvalidaException ex, HttpServletRequest req) {
        return erro(400, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ResponseEntity<ApiError> campos(MethodArgumentNotValidException ex, HttpServletRequest req) {
        var campos = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> new ApiError.CampoInvalido(e.getField(), e.getDefaultMessage())).toList();
        return erro(400, "Existem campos inválidos.", req, campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ResponseEntity<ApiError> restricoes(ConstraintViolationException ex, HttpServletRequest req) {
        var campos = ex.getConstraintViolations().stream()
            .map(e -> new ApiError.CampoInvalido(e.getPropertyPath().toString(), e.getMessage())).toList();
        return erro(400, "Existem parâmetros inválidos.", req, campos);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ResponseEntity<ApiError> parametros(HandlerMethodValidationException ex, HttpServletRequest req) {
        var campos = ex.getParameterValidationResults().stream().flatMap(result ->
            result.getResolvableErrors().stream().map(e -> new ApiError.CampoInvalido(
                result.getMethodParameter().getParameterName(), e.getDefaultMessage()))).toList();
        return erro(400, "Existem parâmetros inválidos.", req, campos);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ResponseEntity<ApiError> formato(Exception ex, HttpServletRequest req) {
        return erro(400, "Requisição inválida. Verifique JSON, campos permitidos, UUIDs, números e datas (AAAA-MM-DD).", req, List.of());
    }

    @ExceptionHandler({DataIntegrityViolationException.class, PessimisticLockingFailureException.class,
        ObjectOptimisticLockingFailureException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    ResponseEntity<ApiError> conflito(Exception ex, HttpServletRequest req) {
        log.warn("Conflito de integridade ou concorrência em {}", req.getRequestURI(), ex);
        return erro(409, "A operação conflita com o estado atual dos dados. Consulte o recurso e tente novamente.", req, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    ResponseEntity<ApiError> metodo(Exception ex, HttpServletRequest req) {
        return erro(405, "Método HTTP não permitido.", req, List.of());
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> status(ResponseStatusException ex, HttpServletRequest req) {
        return erro(ex.getStatusCode().value(), ex.getReason(), req, List.of());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ResponseEntity<ApiError> inesperado(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado em {}", req.getRequestURI(), ex);
        return erro(500, "Erro interno ao processar a requisição.", req, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ResponseEntity<ApiError> rota(Exception ex, HttpServletRequest req) {
        return erro(404, "Recurso não encontrado.", req, List.of());
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    ResponseEntity<ApiError> midia(Exception ex, HttpServletRequest req) {
        return erro(415, "Tipo de conteúdo não suportado. Envie application/json.", req, List.of());
    }

    private ResponseEntity<ApiError> erro(int status, String mensagem, HttpServletRequest req,
                                           List<ApiError.CampoInvalido> campos) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status, mensagem, req.getRequestURI(), campos));
    }
}
