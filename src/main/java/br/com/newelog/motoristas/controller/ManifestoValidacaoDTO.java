package br.com.newelog.motoristas.controller;

public record ManifestoValidacaoDTO(
    String manifestoID,
    String data,
    String nomeMotorista,
    String cpfMotorista,
    String nomeAgregado,
    String cpfCnpjAgregado,
    String placaVeiculo,
    String status,
    Double valorFrete,
    Double totalDespesas,
    Double saldoAPagar
) {}