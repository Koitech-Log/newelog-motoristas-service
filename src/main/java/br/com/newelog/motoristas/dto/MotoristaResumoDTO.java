package br.com.newelog.motoristas.dto;

/**
 * Versão enxuta usada na listagem (GET /api/motoristas) — não traz o histórico
 * de viagens, que só é necessário na tela de detalhe (GET /api/motoristas/{id}).
 * Evita transportar uma lista potencialmente grande de viagens numa resposta
 * que existe só para popular os cards de uma grade.
 */
public record MotoristaResumoDTO(
        Long id,
        String codigoExterno,
        String nome,
        String placaVeiculo,
        String tipoVeiculo,
        String status,
        Integer diasDisponiveis,
        Integer diasOperacao,
        Integer totalViagens,
        boolean cadastroValidado
) {
}
