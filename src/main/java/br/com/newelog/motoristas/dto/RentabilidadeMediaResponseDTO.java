package br.com.newelog.motoristas.dto;

import java.util.List;

public record RentabilidadeMediaResponseDTO(
        int ano,
        int mes,
        RentabilidadeMediaGeralDTO geral,
        List<RentabilidadeMediaItemDTO> porMotorista,
        List<RentabilidadeMediaItemDTO> porVeiculo
) {
}