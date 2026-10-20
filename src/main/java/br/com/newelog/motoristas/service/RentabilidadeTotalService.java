package br.com.newelog.motoristas.service;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.stereotype.Service;

@Service
public class RentabilidadeTotalService {

    public BigDecimal calcular(BigDecimal freteTotal, BigDecimal custoTotal) {
        Objects.requireNonNull(freteTotal, "freteTotal é obrigatório");
        Objects.requireNonNull(custoTotal, "custoTotal é obrigatório");
        return freteTotal.subtract(custoTotal);
    }
}
