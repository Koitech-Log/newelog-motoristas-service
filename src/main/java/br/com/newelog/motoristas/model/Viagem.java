package br.com.newelog.motoristas.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Uma viagem individual dentro de um manifesto. Hoje representa uma linha do
 * manifesto real (granularidade "manifesto = 1 viagem"); a resposta do parceiro
 * indica que um manifesto pode conter várias entregas dentro da mesma rota — essa
 * granularidade mais fina (Entrega, ligada a uma Viagem) é um passo de evolução
 * futura deste modelo, não implementado nesta primeira versão do serviço.
 *
 * Quilometragem não é um campo aqui: os manifestos reais analisados não trazem
 * essa informação de forma confiável (ver "Respostas do Parceiro e Dados Reais",
 * seção 3) — a regra de negócio do parceiro proíbe expor números não confiáveis.
 */
@Entity
@Table(name = "viagem")
@Getter
@Setter
@NoArgsConstructor
public class Viagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorista_id", nullable = false)
    private Motorista motorista;

    @Column(nullable = false)
    private LocalDate data;

    @Column(length = 120)
    private String origem;

    @Column(nullable = false, length = 120)
    private String destino;

    @Column(name = "valor_frete", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorFrete;

    /**
     * Único custo confirmado pelo manifesto de origem no momento — ver regra de
     * negócio do parceiro sobre não estimar valores não confirmados.
     */
    @Column(name = "valor_pedagio", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorPedagio = BigDecimal.ZERO;
}
