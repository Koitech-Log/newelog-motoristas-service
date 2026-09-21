package br.com.newelog.motoristas.service;

import java.util.List;

public class ValidacaoException extends RuntimeException {
    private final List<String> erros;

    public ValidacaoException(List<String> erros) {
        super("Falha de validação");
        this.erros = erros;
    }

    public List<String> getErros() {
        return erros;
    }
}