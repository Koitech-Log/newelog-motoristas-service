package br.com.newelog.motoristas.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.newelog.motoristas.controller.ManifestoValidacaoDTO;
import br.com.newelog.motoristas.dto.ResultadoCruzamentoDTO;
import br.com.newelog.motoristas.model.Motorista;
import br.com.newelog.motoristas.model.Veiculo;
import br.com.newelog.motoristas.repository.MotoristaRepository;
import br.com.newelog.motoristas.repository.VeiculoRepository;

/**
 * Responsabilidade única: cruzar CPF/CNPJ do motorista e placa do veículo
 * vindos do manifesto com a base de dados e informar se já existem.
 * NÃO cadastra nada — isso é responsabilidade de outra etapa.
 */
@Service
public class ManifestoCruzamentoService {

    private final MotoristaRepository motoristaRepository;
    private final VeiculoRepository veiculoRepository;

    public ManifestoCruzamentoService(MotoristaRepository motoristaRepository,
                                       VeiculoRepository veiculoRepository) {
        this.motoristaRepository = motoristaRepository;
        this.veiculoRepository = veiculoRepository;
    }

    @Transactional(readOnly = true)
    public ResultadoCruzamentoDTO verificar(ManifestoValidacaoDTO manifesto) {
        Optional<Motorista> motorista = motoristaRepository
                .findByCpfCnpj(normalizarDocumento(manifesto.cpfMotorista()));

        Optional<Veiculo> veiculo = veiculoRepository
                .findByPlaca(normalizarPlaca(manifesto.placaVeiculo()));

        return new ResultadoCruzamentoDTO(
                motorista.isPresent(),
                motorista.map(Motorista::getId).orElse(null),
                veiculo.isPresent(),
                veiculo.map(Veiculo::getId).orElse(null)
        );
    }

    private String normalizarDocumento(String valor) {
        return valor == null ? null : valor.replaceAll("\\D", "");
    }

    private String normalizarPlaca(String valor) {
        return valor == null ? null : valor.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }
}