package br.com.newelog.motoristas.dto;

import java.math.BigDecimal;

public record RentabilidadeMediaGeralDTO(
        BigDecimal freteTotal,
        BigDecimal custoTotal,
        BigDecimal rentabilidadeTotal,
        long quantidadeViagens,
        BigDecimal rentabilidadeMedia
) {
}