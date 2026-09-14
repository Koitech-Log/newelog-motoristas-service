package br.com.newelog.motoristas.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Versão completa, usada no drawer de detalhe do motorista. valorFrete e
 * valorPedagio somam o período carregado — o cálculo de rentabilidade
 * (frete - pedágio) fica por conta de quem consome a API, para não embutir
 * regra de negócio de exibição dentro do contrato de dados.
 */
public record MotoristaDetalheDTO(
        Long id,
        String codigoExterno,
        String nome,
        String placaVeiculo,
        String tipoVeiculo,
        String marcaVeiculo,
        String status,
        Integer diasDisponiveis,
        Integer diasOperacao,
        BigDecimal valorFreteTotal,
        BigDecimal valorPedagioTotal,
        boolean cadastroValidado,
        List<ViagemResponseDTO> viagens
) {
}
