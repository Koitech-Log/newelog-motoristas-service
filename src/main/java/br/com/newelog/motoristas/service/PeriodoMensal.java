package br.com.newelog.motoristas.service;

import java.time.LocalDate;
import java.time.YearMonth;

public record PeriodoMensal(LocalDate inicio, LocalDate fim) {

    public static PeriodoMensal de(int ano, int mes) {
        YearMonth ym = YearMonth.of(ano, mes); // lança DateTimeException se o mês não for 1-12
        return new PeriodoMensal(ym.atDay(1), ym.plusMonths(1).atDay(1));
    }
}