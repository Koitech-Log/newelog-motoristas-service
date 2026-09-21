package br.com.newelog.motoristas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Nomes de campo em português — o contrato de API "de verdade" não precisa
 * seguir o array mockado do protótipo front-end atual; é o front-end que se
 * adapta a este contrato na migração do mock para a API real.
 */
public record ViagemResponseDTO(
        LocalDate data,
        String origem,
        String destino,
        BigDecimal valorFrete,
        BigDecimal valorPedagio
) {
}
