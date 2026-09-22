package br.com.newelog.motoristas.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record ManifestoValidacaoDTO(
    @NotBlank(message = "manifestoID é obrigatório")
    String manifestoID,

    @NotBlank(message = "data é obrigatória")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "data em formato inválido (esperado yyyy-MM-dd)")
    String data,

    @NotBlank(message = "nomeMotorista é obrigatório")
    String nomeMotorista,

    @NotBlank(message = "cpfMotorista é obrigatório")
    String cpfMotorista,

    @NotBlank(message = "nomeAgregado é obrigatório")
    String nomeAgregado,

    @NotBlank(message = "cpfCnpjAgregado é obrigatório")
    String cpfCnpjAgregado,

    @NotBlank(message = "placaVeiculo é obrigatório")
    @Pattern(regexp = "^[A-Za-z]{3}\\d([A-Za-z]\\d{2}|\\d{3})$", message = "placaVeiculo em formato inválido")
    String placaVeiculo,

    String status,

    @NotNull(message = "valorFrete é obrigatório")
    @PositiveOrZero(message = "valorFrete não pode ser negativo")
    Double valorFrete,

    @NotNull(message = "totalDespesas é obrigatório")
    @PositiveOrZero(message = "totalDespesas não pode ser negativo")
    Double totalDespesas,

    @NotNull(message = "saldoAPagar é obrigatório")
    @PositiveOrZero(message = "saldoAPagar não pode ser negativo")
    Double saldoAPagar
) {}