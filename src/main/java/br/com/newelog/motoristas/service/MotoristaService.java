package br.com.newelog.motoristas.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.newelog.motoristas.dto.MotoristaDetalheDTO;
import br.com.newelog.motoristas.dto.MotoristaResumoDTO;
import br.com.newelog.motoristas.dto.PaginaDTO;
import br.com.newelog.motoristas.dto.ViagemResponseDTO;
import br.com.newelog.motoristas.model.Motorista;
import br.com.newelog.motoristas.model.StatusMotorista;
import br.com.newelog.motoristas.model.Veiculo;
import br.com.newelog.motoristas.repository.MotoristaRepository;
import br.com.newelog.motoristas.repository.MotoristaSpecifications;

@Service
public class MotoristaService {

    private final MotoristaRepository motoristaRepository;

    public MotoristaService(MotoristaRepository motoristaRepository) {
        this.motoristaRepository = motoristaRepository;
    }

    @Transactional(readOnly = true)
    public PaginaDTO<MotoristaResumoDTO> listar(
            StatusMotorista status, String destino, String busca, Pageable pageable
    ) {
        Specification<Motorista> spec = Specification
                .where(MotoristaSpecifications.comStatus(status))
                .and(MotoristaSpecifications.comDestino(destino))
                .and(MotoristaSpecifications.comNomeOuCpfContendo(busca));

        Page<Motorista> pagina = motoristaRepository.findAll(spec, pageable);

        return new PaginaDTO<>(
                pagina.getContent().stream().map(this::paraResumo).toList(),
                pagina.getNumber(),
                pagina.getTotalPages(),
                pagina.getTotalElements(),
                pagina.getSize()
        );
    }

     @Transactional
    public void salvarMotorista(Motorista motorista) {
        motoristaRepository.save(motorista);
    }

    @Transactional(readOnly = true)
    public MotoristaDetalheDTO buscarDetalhe(Long id) {
        Motorista motorista = motoristaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Motorista não encontrado: id=" + id));
        return paraDetalhe(motorista);
    }

    @Transactional
    public MotoristaDetalheDTO atualizarStatus(Long id, StatusMotorista novoStatus) {
        Motorista motorista = motoristaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Motorista não encontrado: id=" + id));
        motorista.setStatus(novoStatus);
        return paraDetalhe(motoristaRepository.save(motorista));
    }

    private MotoristaResumoDTO paraResumo(Motorista m) {
        Veiculo veiculo = m.getVeiculo();
        return new MotoristaResumoDTO(
                m.getId(),
                m.getCodigoExterno(),
                m.getNome(),
                veiculo != null ? veiculo.getPlaca() : null,
                veiculo != null && veiculo.getTipo() != null ? veiculo.getTipo().getNome() : null,
                m.getStatus().name(),
                m.getDiasDisponiveis(),
                m.getDiasOperacao(),
                m.getViagens().size(),
                m.isCadastroValidado()
        );
    }

    private MotoristaDetalheDTO paraDetalhe(Motorista m) {
        Veiculo veiculo = m.getVeiculo();

        BigDecimal totalFrete = m.getViagens().stream()
                .map(v -> v.getValorFrete() == null ? BigDecimal.ZERO : v.getValorFrete())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPedagio = m.getViagens().stream()
                .map(v -> v.getValorPedagio() == null ? BigDecimal.ZERO : v.getValorPedagio())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ViagemResponseDTO> viagens = m.getViagens().stream()
                .map(v -> new ViagemResponseDTO(v.getData(), v.getOrigem(), v.getDestino(), v.getValorFrete(), v.getValorPedagio()))
                .toList();

        return new MotoristaDetalheDTO(
                m.getId(),
                m.getCodigoExterno(),
                m.getNome(),
                m.getCpfCnpj(),
                m.getTelefone(),
                veiculo != null ? veiculo.getPlaca() : null,
                veiculo != null && veiculo.getTipo() != null ? veiculo.getTipo().getNome() : null,
                veiculo != null ? veiculo.getMarca() : null,
                m.getStatus().name(),
                m.getDiasDisponiveis(),
                m.getDiasOperacao(),
                totalFrete,
                totalPedagio,
                totalFrete.subtract(totalPedagio),
                m.isCadastroValidado(),
                viagens
        );
    }
}
