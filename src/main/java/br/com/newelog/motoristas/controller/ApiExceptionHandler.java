package br.com.newelog.motoristas.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.newelog.motoristas.service.ValidacaoException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ValidacaoException.class)
    public ResponseEntity<List<String>> handleValidacao(ValidacaoException ex) {
        return ResponseEntity.badRequest().body(ex.getErros());
    }
}