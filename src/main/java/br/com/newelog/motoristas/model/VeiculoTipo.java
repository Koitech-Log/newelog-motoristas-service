package br.com.newelog.motoristas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Categoria de veículo do cadastro de frota (Fiorino, VUC, Truck, Cavalo mecânico etc.).
 * A lista de categorias reflete o cadastro real de frota da Newelog, não uma
 * classificação inventada — ver csv "veiculos" recebido do parceiro.
 */
@Entity
@Table(name = "veiculo_tipo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VeiculoTipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String nome;
}
