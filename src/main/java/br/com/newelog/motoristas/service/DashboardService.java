package br.com.newelog.motoristas.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.newelog.motoristas.dto.DashboardResponseDTO;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final IndicadoresFinanceirosService indicadoresService;
    private final RentabilidadeTotalService rentabilidadeService;

    public DashboardService(
            IndicadoresFinanceirosService indicadoresService,
            RentabilidadeTotalService rentabilidadeService
    ) {
        this.indicadoresService = indicadoresService;
        this.rentabilidadeService = rentabilidadeService;
    }

    public DashboardResponseDTO buscar(int ano, int mes) {
        BigDecimal freteTotal =
                indicadoresService.calcularFreteTotal(ano, mes);

        BigDecimal custoTotal =
                indicadoresService.calcularCustoTotal(ano, mes);

        BigDecimal rentabilidadeTotal =
                rentabilidadeService.calcular(freteTotal, custoTotal);

        return new DashboardResponseDTO(
                freteTotal,
                custoTotal,
                rentabilidadeTotal
        );
    }
}