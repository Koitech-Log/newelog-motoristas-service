package br.com.newelog.motoristas.dto;

import java.util.List;

public record ResultadoImportacaoVeiculosDTO(
        int cadastrados,
        int atualizados,
        List<Erro> erros
) {

    public record Erro(int linha, String motivo) {
    }
}