package br.com.newelog.motoristas.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.newelog.motoristas.model.Motorista;

/**
 * JpaSpecificationExecutor permite compor filtros dinâmicos (status, destino,
 * texto de busca) sem precisar de um método @Query para cada combinação —
 * ver MotoristaSpecifications.
 */
public interface MotoristaRepository extends JpaRepository<Motorista, Long>,
        JpaSpecificationExecutor<Motorista> {

    Optional<Motorista> findByCodigoExterno(String codigoExterno);

    boolean existsByCodigoExterno(String codigoExterno);
}
