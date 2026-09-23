package br.com.newelog.motoristas.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query(value = "SELECT * FROM motorista WHERE regexp_replace(cpf_cnpj, '[^0-9]', '', 'g') = :cpf",
           nativeQuery = true)
    Optional<Motorista> findByCpfCnpj(@Param("cpf") String cpf);
}
