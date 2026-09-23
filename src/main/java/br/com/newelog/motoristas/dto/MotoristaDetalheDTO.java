package br.com.newelog.motoristas.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Versão completa, usada no drawer de detalhe do motorista.
 *
 * cpfCnpj e telefone: dados pessoais armazenados por pedido explícito do
 * parceiro (ver "Respostas do Parceiro e Dados Reais", pergunta 9). telefone
 * pode vir nulo — não está disponível na fonte de dados processada até agora.
 *
 * rentabilidadeTotal = valorFreteTotal - valorPedagioTotal. É calculada aqui
 * (não no cliente) para não duplicar essa regra de negócio em cada front-end
 * que consumir esta API. IMPORTANTE: o pedágio é hoje o único custo
 * confirmado pelo manifesto de origem — combustível, manutenção e comissão
 * ainda não são registrados por motorista, então este valor subestima o
 * custo real da operação (mesma ressalva já feita no protótipo front-end).
 */
public record MotoristaDetalheDTO(
        Long id,
        String codigoExterno,
        String nome,
        String cpfCnpj,
        String telefone,
        String placaVeiculo,
        String tipoVeiculo,
        String marcaVeiculo,
        String status,
        Integer diasDisponiveis,
        Integer diasOperacao,
        BigDecimal valorFreteTotal,
        BigDecimal valorPedagioTotal,
        BigDecimal rentabilidadeTotal,
        boolean cadastroValidado,
        List<ViagemResponseDTO> viagens
) {
}
