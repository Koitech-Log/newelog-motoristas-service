package br.com.newelog.motoristas.dto;

import java.util.List;

/**
 * Resultado do cadastro de veículos em lote: quantos foram cadastrados e quais
 * linhas foram rejeitadas, com o motivo de cada uma.
 */
public record ResultadoImportacaoVeiculosDTO(
        int cadastrados,
        List<Erro> erros
) {

    public record Erro(int linha, String motivo) {
    }
}