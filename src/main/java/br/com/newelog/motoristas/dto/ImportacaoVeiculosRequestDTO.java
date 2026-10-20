
package br.com.newelog.motoristas.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

/**
 * Corpo esperado por POST /api/veiculos/cadastrar-de-csv, chamado pelo
 * manifesto-service depois de ler o CSV de veículos. Cada item guarda o número
 * da linha no arquivo original, para que os erros possam apontar a linha certa.
 */
    public record ImportacaoVeiculosRequestDTO(
        @NotEmpty(message = "veiculos é obrigatório e não pode ser vazio")
        List<Item> veiculos
) {

    public record Item(int linha, String placa, String marca, String tipo) {
    }
}
