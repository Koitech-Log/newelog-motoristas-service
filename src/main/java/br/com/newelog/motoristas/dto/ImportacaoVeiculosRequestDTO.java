
package br.com.newelog.motoristas.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Corpo esperado por POST /api/veiculos/cadastrar-de-csv, chamado pelo
 * manifesto-service depois de ler o CSV de veículos. Cada item guarda o número
 * da linha no arquivo original, para que os erros possam apontar a linha certa.
 */
    public record ImportacaoVeiculosRequestDTO(
        @NotEmpty(message = "veiculos é obrigatório e não pode ser vazio")
        List<Item> veiculos
) {

        public record Item(
            int linha,
            @NotBlank(message = "placa é obrigatória")
            @Pattern(regexp = "^[A-Za-z]{3}-?\\d([A-Za-z]\\d{2}|\\d{3})$",
                    message = "placa em formato inválido (esperado ABC1234 ou ABC1D23)")
            String placa,
            @Size(max = 60, message = "marca deve ter no máximo 60 caracteres")
            String marca,
            @NotBlank(message = "tipo é obrigatório")
            String tipo
    ) {
    }
}
