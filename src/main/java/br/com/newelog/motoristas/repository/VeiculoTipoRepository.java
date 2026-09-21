package br.com.newelog.motoristas.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.newelog.motoristas.model.VeiculoTipo;

public interface VeiculoTipoRepository extends JpaRepository<VeiculoTipo, Long> {

    Optional<VeiculoTipo> findByNome(String nome);
}
