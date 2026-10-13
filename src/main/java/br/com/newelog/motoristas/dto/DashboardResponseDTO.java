package br.com.newelog.motoristas.dto;

import java.math.BigDecimal;

public record DashboardResponseDTO(
        BigDecimal freteTotal,
        BigDecimal custoTotal,
        BigDecimal rentabilidadeTotal
) {
}