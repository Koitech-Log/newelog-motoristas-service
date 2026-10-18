package br.com.newelog.motoristas.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.newelog.motoristas.dto.RentabilidadeMediaGeralDTO;
import br.com.newelog.motoristas.dto.RentabilidadeMediaItemDTO;
import br.com.newelog.motoristas.dto.RentabilidadeMediaResponseDTO;
import br.com.newelog.motoristas.repository.ViagemRepository;

@Service
@Transactional(readOnly = true)
public class RentabilidadeAnaliseService {

    private final IndicadoresFinanceirosService indicadoresService;
    private final ViagemRepository viagemRepository;
    private final RentabilidadeTotalService rentabilidadeTotalService;
    private final RentabilidadeMediaService rentabilidadeMediaService;

    public RentabilidadeAnaliseService(
            IndicadoresFinanceirosService indicadoresService,
            ViagemRepository viagemRepository,
            RentabilidadeTotalService rentabilidadeTotalService,
            RentabilidadeMediaService rentabilidadeMediaService
    ) {
        this.indicadoresService = indicadoresService;
        this.viagemRepository = viagemRepository;
        this.rentabilidadeTotalService = rentabilidadeTotalService;
        this.rentabilidadeMediaService = rentabilidadeMediaService;
    }

    public RentabilidadeMediaResponseDTO buscar(int ano, int mes) {
        PeriodoMensal p = PeriodoMensal.de(ano, mes);

        BigDecimal frete = indicadoresService.calcularFreteTotal(ano, mes);
        BigDecimal custo = indicadoresService.calcularCustoTotal(ano, mes);
        long viagens = indicadoresService.contarViagens(ano, mes);
        BigDecimal rentabilidade = rentabilidadeTotalService.calcular(frete, custo);

        RentabilidadeMediaGeralDTO geral = new RentabilidadeMediaGeralDTO(
                frete, custo, rentabilidade, viagens,
                rentabilidadeMediaService.calcular(rentabilidade, viagens));

        return new RentabilidadeMediaResponseDTO(
                ano, mes, geral,
                montar(viagemRepository.rentabilidadePorMotorista(p.inicio(), p.fim()), "Sem nome"),
                montar(viagemRepository.rentabilidadePorVeiculo(p.inicio(), p.fim()), "Sem veículo"));
    }

    private List<RentabilidadeMediaItemDTO> montar(List<Object[]> linhas, String nomePadrao) {
        return linhas.stream()
                .map(l -> paraItem(l, nomePadrao))
                .sorted(Comparator.comparing(RentabilidadeMediaItemDTO::rentabilidadeMedia).reversed()
                        .thenComparing(RentabilidadeMediaItemDTO::nome))
                .toList();
    }

    private RentabilidadeMediaItemDTO paraItem(Object[] l, String nomePadrao) {
        Long id = l[0] == null ? null : ((Number) l[0]).longValue();
        String nome = l[1] == null ? nomePadrao : (String) l[1];
        BigDecimal frete = (BigDecimal) l[2];
        BigDecimal custo = (BigDecimal) l[3];
        long viagens = ((Number) l[4]).longValue();

        BigDecimal rentabilidade = rentabilidadeTotalService.calcular(frete, custo);
        return new RentabilidadeMediaItemDTO(id, nome, frete, custo, rentabilidade, viagens,
                rentabilidadeMediaService.calcular(rentabilidade, viagens));
    }
}