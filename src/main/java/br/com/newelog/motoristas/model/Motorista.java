package br.com.newelog.motoristas.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Motorista agregado. É a pessoa que efetivamente dirige — distinta do "Agregado"
 * do manifesto de origem, que é o titular do contrato/CNPJ do veículo e nem sempre
 * é a mesma pessoa (ver documento "Respostas do Parceiro e Dados Reais", seção 3).
 *
 * Dados pessoais sensíveis do manifesto de origem (CPF, PIS, data de nascimento,
 * endereço) não têm campo equivalente aqui, por decisão de produto — este serviço
 * não é o sistema de RH/cadastro trabalhista, é o painel operacional de frota.
 */
@Entity
@Table(name = "motorista")
@Getter
@Setter
@NoArgsConstructor
public class Motorista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identificador estável vindo do manifesto (ex: slug do nome), usado para
     * casar um motorista já cadastrado com o que aparece num manifesto importado
     * futuramente. Não é o ID técnico da tabela.
     */
    @Column(name = "codigo_externo", unique = true, length = 120)
    private String codigoExterno;

    @Column(nullable = false, length = 150)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id")
    private Veiculo veiculo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusMotorista status;

    @Column(name = "dias_disponiveis", nullable = false)
    private Integer diasDisponiveis;

    @Column(name = "dias_operacao", nullable = false)
    private Integer diasOperacao;

    /**
     * Falso quando o motorista foi incluído automaticamente a partir de um
     * manifesto importado e ainda não passou por validação manual do cadastro
     * (regra definida pelo parceiro — ver "Respostas do Parceiro", seção 2).
     */
    @Column(name = "cadastro_validado", nullable = false)
    private boolean cadastroValidado = true;

    @OneToMany(mappedBy = "motorista", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Viagem> viagens = new ArrayList<>();
}
