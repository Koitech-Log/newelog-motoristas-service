package br.com.newelog.motoristas.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.newelog.motoristas.model.Viagem;

public interface ViagemRepository extends JpaRepository<Viagem, Long> {

    @Query("""
        SELECT COALESCE(SUM(v.valorFrete), 0)
        FROM Viagem v
        WHERE v.data >= :inicio AND v.data < :fim
        """)
    BigDecimal somarFretePorPeriodo(@Param("inicio") LocalDate inicio,
                                    @Param("fim") LocalDate fim);

    @Query("""
        SELECT COALESCE(SUM(v.valorPedagio), 0)
        FROM Viagem v
        WHERE v.data >= :inicio AND v.data < :fim
        """)
    BigDecimal somarPedagioPorPeriodo(@Param("inicio") LocalDate inicio,
                                      @Param("fim") LocalDate fim);

    @Query("""
        SELECT COUNT(v)
        FROM Viagem v
        WHERE v.data >= :inicio AND v.data < :fim
        """)
    long contarViagensPorPeriodo(@Param("inicio") LocalDate inicio,
                                 @Param("fim") LocalDate fim);
}