package br.com.newelog.motoristas.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.newelog.motoristas.repository.ViagemRepository;

@Service
@Transactional(readOnly = true)
public class IndicadoresFinanceirosService {

    private final ViagemRepository viagemRepository;

    public IndicadoresFinanceirosService(ViagemRepository viagemRepository) {
        this.viagemRepository = viagemRepository;
    }

    /** Task 2:*/
    public BigDecimal calcularCustoTotal(int ano, int mes) {
        PeriodoMensal p = PeriodoMensal.de(ano, mes);
        return zeroSeNulo(viagemRepository.somarPedagioPorPeriodo(p.inicio(), p.fim()));
    }

    /** Task 3*/
    public BigDecimal calcularFreteTotal(int ano, int mes) {
        PeriodoMensal p = PeriodoMensal.de(ano, mes);
        return zeroSeNulo(viagemRepository.somarFretePorPeriodo(p.inicio(), p.fim()));
    }

    private BigDecimal zeroSeNulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}