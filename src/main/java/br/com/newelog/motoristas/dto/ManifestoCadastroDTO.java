package br.com.newelog.motoristas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

/**
 * Corpo esperado por POST /api/motoristas quando chamado pelo manifesto-service
 * para efetivar o cadastro de uma linha de manifesto já validada (ver
 * POST /api/motoristas/validar-manifesto, chamado antes deste). Espelha os
 * mesmos campos de ManifestoValidacaoDTO, mais a data já convertida para
 * LocalDate — o manifesto-service já entrega a data no formato ISO
 * (yyyy-MM-dd) depois de interpretar o dd/MM/yyyy do CSV.
 *
 * Não reaproveita a entidade Motorista diretamente como corpo da requisição:
 * o relacionamento Motorista -> Veiculo não tem cascade (ver model.Motorista),
 * então um veículo novo enviado como objeto aninhado falharia ao persistir.
 * Este DTO deixa explícito que o service é quem resolve (ou cria) o veículo
 * pela placa.
 */
public record ManifestoCadastroDTO(
        @NotBlank(message = "codigoExterno é obrigatório")
        String codigoExterno,

        @NotNull(message = "data é obrigatória")
        LocalDate data,

        @NotBlank(message = "nomeMotorista é obrigatório")
        String nomeMotorista,

        @NotBlank(message = "cpfMotorista é obrigatório")
        @Pattern(regexp = "^\\d{11}$|^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$", message = "CPF do motorista em formato inválido")
        String cpfMotorista,

        @NotBlank(message = "placaVeiculo é obrigatório")
        String placaVeiculo,

        String status,

        @NotNull(message = "valorFrete é obrigatório")
        @PositiveOrZero(message = "valorFrete não pode ser negativo")
        Double valorFrete
) {
}
