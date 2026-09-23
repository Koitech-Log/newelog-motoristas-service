package br.com.newelog.motoristas.dto;

public record ResultadoCruzamentoDTO(
    boolean motoristaCadastrado,
    Long motoristaId,
    boolean veiculoCadastrado,
    Long veiculoId
) {}