package br.com.newelog.motoristas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.newelog.motoristas.config.ValorizacaoProperties;

class ValorizacaoViagemServiceTest {

    private ValorizacaoViagemService servico(String fator) {
        return new ValorizacaoViagemService(new ValorizacaoProperties(new BigDecimal(fator)));
    }

    @Test
    void destinoSaoPauloRecebeValorizacao() {
        assertEquals(new BigDecimal("120.00"), servico("1.20").aplicar(new BigDecimal("100.00"), "São Paulo"));
    }

    @Test
    void ignoraAcentoEMaiusculas() {
        ValorizacaoViagemService s = servico("1.20");
        assertEquals(new BigDecimal("120.00"), s.aplicar(new BigDecimal("100.00"), "SAO PAULO"));
        assertEquals(new BigDecimal("120.00"), s.aplicar(new BigDecimal("100.00"), "  são paulo/SP "));
    }

    @Test
    void outraCidadeUsaValorBase() {
        assertEquals(new BigDecimal("100.00"), servico("1.20").aplicar(new BigDecimal("100.00"), "Campinas"));
    }

    @Test
    void destinoNuloUsaValorBase() {
        assertEquals(new BigDecimal("100.00"), servico("1.20").aplicar(new BigDecimal("100.00"), null));
    }

    @Test
    void fatorVemDaConfiguracao() {
        assertEquals(new BigDecimal("150.00"), servico("1.50").aplicar(new BigDecimal("100.00"), "São Paulo"));
    }

    @Test
    void valorBaseNuloRetornaZero() {
        assertEquals(new BigDecimal("0.00"), servico("1.20").aplicar(null, "São Paulo"));
    }
}