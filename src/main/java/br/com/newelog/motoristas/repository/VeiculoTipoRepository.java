package br.com.newelog.motoristas.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.newelog.motoristas.model.VeiculoTipo;

public interface VeiculoTipoRepository extends JpaRepository<VeiculoTipo, Long> {

    Optional<VeiculoTipo> findByNome(String nome);

    /**
     * Usado na importação de CSV de veículos, onde "fiorino" e "Fiorino"
     * precisam casar com o mesmo tipo cadastrado.
     */
    Optional<VeiculoTipo> findByNomeIgnoreCase(String nome);
}