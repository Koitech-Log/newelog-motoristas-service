package br.com.newelog.motoristas.dto;

import br.com.newelog.motoristas.model.StatusMotorista;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Corpo de POST /api/motoristas (cadastro manual, perfil Gestor). Substitui o
 * uso da entidade Motorista como corpo da requisição: id, viagens e
 * cadastroValidado não podem ser definidos pelo cliente. O veículo é
 * referenciado por id (um veículo novo entra pelo fluxo de manifesto).
 */
public record MotoristaCadastroRequestDTO(
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 150, message = "nome deve ter no máximo 150 caracteres")
        String nome,

        @Size(max = 120, message = "codigoExterno deve ter no máximo 120 caracteres")
        String codigoExterno,

        @Pattern(regexp = "^\\d{11}$|^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$|^\\d{14}$|^\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}$",
                message = "cpfCnpj em formato inválido")
        String cpfCnpj,

        @Size(max = 20, message = "telefone deve ter no máximo 20 caracteres")
        String telefone,

        Long veiculoId,

        StatusMotorista status,

        @PositiveOrZero(message = "diasDisponiveis não pode ser negativo")
        Integer diasDisponiveis,

        @PositiveOrZero(message = "diasOperacao não pode ser negativo")
        Integer diasOperacao
) {
}
