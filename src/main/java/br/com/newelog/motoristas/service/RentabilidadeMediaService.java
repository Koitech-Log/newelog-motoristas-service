package br.com.newelog.motoristas.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import org.springframework.stereotype.Service;

@Service
public class RentabilidadeMediaService {

    public BigDecimal calcular(BigDecimal rentabilidadeTotal, long quantidadeViagens) {
        Objects.requireNonNull(rentabilidadeTotal, "rentabilidadeTotal é obrigatória");
        if (quantidadeViagens <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return rentabilidadeTotal.divide(
                BigDecimal.valueOf(quantidadeViagens), 2, RoundingMode.HALF_UP);
    }
}