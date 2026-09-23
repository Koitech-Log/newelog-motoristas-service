package br.com.newelog.motoristas.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.newelog.motoristas.service.RecursoNaoEncontradoException;
import br.com.newelog.motoristas.service.ValidacaoException;

/**
 * Converte exceções de domínio em respostas HTTP consistentes, com o mesmo
 * formato de corpo de erro em todos os endpoints deste serviço.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(corpoDeErro(ex.getMessage()));
    }

    @ExceptionHandler(ValidacaoException.class)
    public ResponseEntity<Map<String, Object>> handleValidacaoNegocio(ValidacaoException ex) {
        Map<String, Object> corpo = corpoDeErro("Falha na validação das regras de negócio.");
        corpo.put("erros", ex.getErros());
        return ResponseEntity.badRequest().body(corpo);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidacao(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .orElse("Dados inválidos.");
        return ResponseEntity.badRequest().body(corpoDeErro(mensagem));
    }

    private Map<String, Object> corpoDeErro(String mensagem) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("timestamp", Instant.now().toString());
        corpo.put("mensagem", mensagem);
        return corpo;
    }
}
