package br.com.newelog.motoristas.dto;

import java.math.BigDecimal;

public record RentabilidadeMediaItemDTO(
        Long id,
        String nome,
        BigDecimal freteTotal,
        BigDecimal custoTotal,
        BigDecimal rentabilidadeTotal,
        long quantidadeViagens,
        BigDecimal rentabilidadeMedia
) {
}