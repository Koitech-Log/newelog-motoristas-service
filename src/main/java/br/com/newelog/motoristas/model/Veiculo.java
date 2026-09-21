package br.com.newelog.motoristas.model;

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
 * Veículo da frota agregada, identificado pela placa.
 * Campos sensíveis do cadastro de origem (Renavam, Chassi, valores de compra/venda)
 * são deliberadamente omitidos aqui — não agregam valor à operação do dashboard
 * e são dados documentais/financeiros da empresa sem necessidade de exposição via API.
 */
@Entity
@Table(name = "veiculo")
@Getter
@Setter
@NoArgsConstructor
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String placa;

    @Column(length = 60)
    private String marca;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_tipo_id")
    private VeiculoTipo tipo;
}
