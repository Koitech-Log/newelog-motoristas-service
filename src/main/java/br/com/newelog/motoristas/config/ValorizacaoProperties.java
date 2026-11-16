package br.com.newelog.motoristas.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

    @ConfigurationProperties(prefix = "app.ranking.valorizacao")
public record ValorizacaoProperties(BigDecimal fatorSaoPaulo) {}

